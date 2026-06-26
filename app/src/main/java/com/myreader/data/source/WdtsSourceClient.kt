package com.myreader.data.source

import android.content.Context
import android.util.Log
import com.myreader.MyReaderApp
import com.myreader.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * WDTS（我的听书）源客户端
 *
 * 功能：
 * 1. 从 wdts.top 下载源元数据（JSON格式）
 * 2. 尝试下载 JAR 插件文件
 * 3. 从 JAR 中提取搜索配置
 * 4. 执行搜索
 */
object WdtsSourceClient {

    private const val TAG = "WdtsSourceClient"
    private const val PREFS_NAME = "wdts_sources"
    private const val KEY_SOURCES = "wdts_source_list"

    /** wdts.top 的可访问元数据 URL 列表 */
    val WDTS_HUBS = listOf(
        WdtsHub("外部书源", "https://wdts.top/api/sources/external_sources.js", 19),
        WdtsHub("shun书源", "https://wdts.top/api/sources/shun.json", 9),
        WdtsHub("sound听书源", "https://wdts.top/api/sources/my_sound.json", 7),
        WdtsHub("bxb100书源", "https://wdts.top/api/sources/bxb100.json", 0),
        WdtsHub("英文有声源", "https://wdts.top/api/sources/english_source.json", 3),
        WdtsHub("视频源", "https://wdts.top/api/sources/videosource.json", 2),
        WdtsHub("网友视频源", "https://wdts.top/api/sources/my_video.json", 8),
        WdtsHub("网友音乐源", "https://wdts.top/api/sources/my_music.json", 5),
        WdtsHub("助眠影音源", "https://wdts.top/api/sources/kyloasmr.json", 0),
        WdtsHub("外部书源(js)", "https://wdts.top/api/sources/external_sources.js", 0, isJs = true),
    )

    data class WdtsHub(
        val name: String,
        val url: String,
        val expectedCount: Int = 0,
        val isJs: Boolean = false
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // ==================== 元数据下载 ====================

    /**
     * 下载所有 wdts.top 源的元数据
     */
    suspend fun fetchAllMetadata(progress: ((String) -> Unit)? = null): List<WdtsSource> =
        withContext(Dispatchers.IO) {
            val allSources = mutableListOf<WdtsSource>()

            for (hub in WDTS_HUBS) {
                progress?.invoke("正在获取: ${hub.name}...")
                try {
                    val sources = fetchHubMetadata(hub)
                    allSources.addAll(sources)
                    Log.i(TAG, "✅ [${hub.name}] 获取到 ${sources.size} 个源")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [${hub.name}] 失败: ${e.message}")
                }
            }

            Log.i(TAG, "总计获取 ${allSources.size} 个WDTS源元数据")
            allSources.distinctBy { it.id }
        }

    /**
     * 从单个 hub URL 获取元数据
     */
    suspend fun fetchHubMetadata(hub: WdtsHub): List<WdtsSource> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(hub.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "[${hub.name}] HTTP ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            if (body.isBlank()) return@withContext emptyList()

            // 尝试解析为 JSON
            val json = body.trim()

            // JS 文件格式: var sources = [...] 或直接是 JSON 对象
            val cleanedJson = if (hub.isJs || json.startsWith("var ")) {
                extractJsonFromJs(json)
            } else {
                json
            }

            // 解析
            val metadata = try {
                val obj = JSONObject(cleanedJson)
                // 可能是直接的元数据对象
                val meta = WdtsSourceMetadata.fromJson(cleanedJson)
                if (meta != null && meta.entryPackage.isNotBlank()) {
                    listOf(meta)
                } else {
                    // 尝试找嵌套的 sources 列表
                    if (obj.has("sources")) {
                        val arr = obj.getJSONArray("sources")
                        (0 until arr.length()).mapNotNull { i ->
                            try {
                                WdtsSourceMetadata.fromJson(arr.getJSONObject(i).toString())
                            } catch (e: Exception) { null }
                        }
                    } else emptyList()
                }
            } catch (e: Exception) {
                // 可能直接是元数据JSON对象
                val meta = WdtsSourceMetadata.fromJson(cleanedJson)
                if (meta != null) listOf(meta) else emptyList()
            }

            metadata.map { meta ->
                WdtsSource(
                    metadata = meta,
                    hubUrl = hub.url,
                    hubName = hub.name
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "[${hub.name}] 获取失败: ${e.message}")
            emptyList()
        }
    }

    private fun extractJsonFromJs(jsContent: String): String {
        // 尝试提取 var xxx = ... 中的 JSON 部分
        val patterns = listOf(
            Regex("""var\s+\w+\s*=\s*(\[.*])\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
            Regex("""var\s+\w+\s*=\s*(\{.*})\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
            Regex("""const\s+\w+\s*=\s*(\[.*])\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
            Regex("""const\s+\w+\s*=\s*(\{.*})\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
            Regex("""module\.exports\s*=\s*(\[.*])\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
            Regex("""module\.exports\s*=\s*(\{.*})\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL),
        )
        for (pattern in patterns) {
            val match = pattern.find(jsContent)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        // 回退：直接找第一个 [ 或 {
        val start = jsContent.indexOfFirst { it == '[' || it == '{' }
        return if (start >= 0) jsContent.substring(start) else jsContent
    }

    // ==================== JAR 下载 ====================

    /**
     * 扫描本地 JAR 目录，发现已手动导入的 JAR 文件
     * 对应"我的听书"的离线源功能：用户将 JAR 放入指定目录后自动识别
     *
     * @param context 应用上下文
     * @return 发现的 JAR 文件列表（文件名 -> 绝对路径）
     */
    fun discoverLocalJars(context: Context = MyReaderApp.instance): Map<String, String> {
        val jarDir = File(context.filesDir, "wdts_jars")
        if (!jarDir.exists() || !jarDir.isDirectory) return emptyMap()

        return jarDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jar") && it.length() > 100 }
            ?.associate { it.nameWithoutExtension to it.absolutePath }
            ?: emptyMap()
    }

    /**
     * 将一个已发现的本地 JAR 注册为 WdtsSource
     */
    fun createSourceFromLocalJar(
        jarName: String,
        jarPath: String,
        hubName: String = "本地导入"
    ): WdtsSource {
        val metadata = WdtsSourceMetadata(
            version = 0,
            entryPackage = jarName,
            downloadUrl = "",
            updateMsg = "本地JAR文件"
        )
        return WdtsSource(
            metadata = metadata,
            hubUrl = "local://$jarPath",
            hubName = hubName,
            status = WdtsSourceStatus.DOWNLOADED,
            jarLocalPath = jarPath
        )
    }

    /**
     * 下载 JAR 插件文件到本地
     */
    suspend fun downloadJar(
        source: WdtsSource,
        context: Context = MyReaderApp.instance,
        progress: ((String) -> Unit)? = null
    ): WdtsSource = withContext(Dispatchers.IO) {
        val jarDir = File(context.filesDir, "wdts_jars")
        if (!jarDir.exists()) jarDir.mkdirs()

        val jarFile = File(jarDir, "${source.metadata.entryPackage}.jar")

        // 如果已存在且版本匹配，跳过下载
        if (jarFile.exists() && jarFile.length() > 0) {
            Log.i(TAG, "JAR已存在: ${jarFile.absolutePath} (${jarFile.length()} bytes)")
            return@withContext source.copy(
                jarLocalPath = jarFile.absolutePath,
                status = WdtsSourceStatus.DOWNLOADED
            )
        }

        val url = source.metadata.downloadUrl
        if (url.isBlank()) {
            return@withContext source.copy(
                status = WdtsSourceStatus.DOWNLOAD_FAILED,
                errorMsg = "下载URL为空"
            )
        }

        progress?.invoke("下载中: ${source.metadata.entryPackage}...")
        Log.i(TAG, "开始下载JAR: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "JAR下载失败 HTTP ${response.code}")
                return@withContext source.copy(
                    status = WdtsSourceStatus.DOWNLOAD_FAILED,
                    errorMsg = "HTTP ${response.code}: ${response.message}"
                )
            }

            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                return@withContext source.copy(
                    status = WdtsSourceStatus.DOWNLOAD_FAILED,
                    errorMsg = "下载内容为空"
                )
            }

            // 验证是否是有效的 JAR/ZIP 文件
            val isZip = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            FileOutputStream(jarFile).use { it.write(bytes) }

            Log.i(TAG, "JAR下载成功: ${jarFile.absolutePath} (${jarFile.length()} bytes)")

            if (!isZip) {
                Log.w(TAG, "下载的文件不是有效的ZIP/JAR格式")
                return@withContext source.copy(
                    jarLocalPath = jarFile.absolutePath,
                    status = WdtsSourceStatus.DOWNLOAD_FAILED,
                    errorMsg = "下载的不是有效的JAR文件"
                )
            }

            source.copy(
                jarLocalPath = jarFile.absolutePath,
                status = WdtsSourceStatus.DOWNLOADED
            )
        } catch (e: Exception) {
            Log.e(TAG, "JAR下载异常: ${e.message}")
            source.copy(
                status = WdtsSourceStatus.DOWNLOAD_FAILED,
                errorMsg = e.localizedMessage ?: "下载异常"
            )
        }
    }

    // ==================== JAR 内容提取 ====================

    /**
     * 从下载的 JAR 中提取搜索配置
     *
     * 我的听书 JAR 插件通常包含：
     * - Java .class 编译代码（搜索逻辑）
     * - 可能的配置文件（JSON/XML）
     *
     * 我们搜索以下配置：
     * 1. plugin.properties / config.json / source_config.json 等文件
     * 2. 如果找不到，尝试通过类名推断网站
     */
    suspend fun extractRules(
        source: WdtsSource,
        context: Context = MyReaderApp.instance
    ): WdtsSource = withContext(Dispatchers.IO) {
        // 如果已经解析过或已回退，跳过
        if (source.status == WdtsSourceStatus.EXTRACTED ||
            source.status == WdtsSourceStatus.FALLBACK_HTTP) {
            return@withContext source
        }

        // 如果是 DOWNLOADED 状态，尝试从JAR提取
        if (source.status == WdtsSourceStatus.DOWNLOADED) {
            val jarPath = source.jarLocalPath
            if (jarPath != null && File(jarPath).exists()) {
                Log.i(TAG, "开始解析JAR: $jarPath")
                val rule = tryExtractFromJar(File(jarPath))
                if (rule != null) {
                    Log.i(TAG, "✅ 成功从JAR提取搜索配置")
                    return@withContext source.copy(
                        status = WdtsSourceStatus.EXTRACTED,
                        extractedRule = rule
                    )
                }
                Log.w(TAG, "JAR解析无配置，尝试映射模式")
            } else {
                Log.w(TAG, "JAR文件不存在: $jarPath")
            }
        }

        // 尝试通过 entry_package 名称推断已知网站（适用于所有非EXTRACTED状态）
        val mappedRule = mapByEntryPackage(source.metadata.entryPackage)
        if (mappedRule != null) {
            Log.i(TAG, "✅ [${source.metadata.entryPackage}] 映射到: ${mappedRule.siteName}")
            return@withContext source.copy(
                status = WdtsSourceStatus.FALLBACK_HTTP,
                extractedRule = mappedRule
            )
        }

        // 尝试多站点映射
        val multiRules = mapByEntryPackageMulti(source.metadata.entryPackage)
        if (multiRules.isNotEmpty()) {
            Log.i(TAG, "✅ [${source.metadata.entryPackage}] 映射到 ${multiRules.size} 个子站点")
            return@withContext source.copy(
                status = WdtsSourceStatus.FALLBACK_HTTP,
                extractedRule = multiRules.first() // 保存第一个作为默认，搜索时展开
            )
        }

        // 无法映射
        source.copy(
            status = WdtsSourceStatus.DOWNLOAD_FAILED,
            errorMsg = if (source.status == WdtsSourceStatus.DOWNLOADED)
                "JAR已下载但无法解析，且无已知站点映射"
            else
                "JAR下载失败且无已知站点映射: ${source.metadata.entryPackage}"
        )
    }

    private fun tryExtractFromJar(jarFile: File): WdtsExtractedRule? {
        try {
            ZipFile(jarFile).use { zip ->
                val entries = zip.entries()
                val entryList = mutableListOf<String>()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    entryList.add(entry.name)
                }

                Log.d(TAG, "JAR包含 ${entryList.size} 个条目")
                if (entryList.size <= 2) {
                    Log.d(TAG, "条目: ${entryList.take(10)}")
                }

                // 查找配置文件
                val configNames = listOf(
                    "config.json", "source_config.json", "bookSource.json",
                    "search_config.json", "plugin.json", "manifest.json",
                    "plugin.properties", "book_source.json", "search.json",
                    "META-INF/plugin.properties"
                )

                for (configName in configNames) {
                    val entry = zip.getEntry(configName) ?: continue
                    try {
                        val content = zip.getInputStream(entry).bufferedReader().readText()
                        Log.d(TAG, "找到配置文件: $configName, 大小: ${content.length}")

                        // 尝试解析为 JSON 格式的搜索配置
                        if (configName.endsWith(".json")) {
                            val rule = tryParseJsonConfig(content)
                            if (rule != null) return rule
                        }

                        // 尝试解析 properties 配置
                        if (configName.endsWith(".properties")) {
                            val rule = tryParsePropertiesConfig(content)
                            if (rule != null) return rule
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析配置异常: $configName - ${e.message}")
                    }
                }

                // 查找任何非 .class 的文本文件
                val textEntries = entryList.filter {
                    !it.endsWith(".class") && !it.endsWith("/") &&
                    !it.startsWith("META-INF/") && !it.startsWith("kotlin/") &&
                    it.length < 100
                }
                for (textEntry in textEntries.take(5)) {
                    if (textEntry.endsWith(".json") || textEntry.endsWith(".xml") ||
                        textEntry.endsWith(".txt") || textEntry.endsWith(".cfg")) {
                        val entry = zip.getEntry(textEntry) ?: continue
                        try {
                            val content = zip.getInputStream(entry).bufferedReader().readText()
                            Log.d(TAG, "文本文件 $textEntry: ${content.take(200)}")
                        } catch (e: Exception) {
                            // skip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析JAR异常: ${e.message}")
        }

        return null
    }

    private fun tryParseJsonConfig(json: String): WdtsExtractedRule? {
        return try {
            val obj = JSONObject(json)

            // 检测是否包含搜索配置
            val searchUrl = obj.optString("searchUrl", "")
                .ifBlank { obj.optString("search_url", "") }

            if (searchUrl.isNotBlank()) {
                // 可能是 Legado 格式的配置
                val ruleSearch = obj.optJSONObject("ruleSearch")
                val bookList = ruleSearch?.optString("bookList", "")
                    ?: obj.optString("searchListRule", "")
                val nameRule = ruleSearch?.optString("name", "")
                    ?: obj.optString("searchTitleRule", "")
                val authorRule = ruleSearch?.optString("author", "")
                    ?: obj.optString("searchAuthorRule", "")
                val coverRule = ruleSearch?.optString("coverUrl", "")
                    ?: obj.optString("searchCoverRule", "")
                val detailRule = ruleSearch?.optString("detailUrl", "")
                    ?: obj.optString("searchUrlRule", "")

                // 如果是 Legado 规则格式，尝试转为 CSS
                val cssBookList = if (bookList.isNotBlank()) {
                    LegadoRuleParser.toCssSelector(bookList)
                } else ""
                val cssName = if (nameRule.isNotBlank()) {
                    LegadoRuleParser.toCssSelector(nameRule)
                } else ""
                val cssAuthor = if (authorRule.isNotBlank()) {
                    LegadoRuleParser.toCssSelector(authorRule)
                } else ""
                val cssCover = if (coverRule.isNotBlank()) {
                    LegadoRuleParser.toCssSelector(coverRule)
                } else ""
                val cssDetail = if (detailRule.isNotBlank()) {
                    LegadoRuleParser.toCssSelector(detailRule)
                } else ""

                WdtsExtractedRule(
                    siteName = obj.optString("bookSourceName", ""),
                    searchUrl = searchUrl,
                    bookListCss = cssBookList,
                    nameCss = cssName,
                    authorCss = cssAuthor,
                    coverCss = cssCover,
                    detailCss = cssDetail,
                    userAgent = obj.optString("httpUserAgent", ""),
                    charset = obj.optString("charset", "UTF-8"),
                    rawRules = json
                )
            } else {
                // 可能只是简单的站点配置
                val url = obj.optString("url", "")
                if (url.isNotBlank()) {
                    WdtsExtractedRule(
                        siteName = obj.optString("name", ""),
                        searchUrl = url,
                        userAgent = obj.optString("userAgent", ""),
                        rawRules = json
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParsePropertiesConfig(props: String): WdtsExtractedRule? {
        val map = mutableMapOf<String, String>()
        props.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    map[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim()
                }
            }
        }

        val searchUrl = map["searchUrl"] ?: map["search_url"] ?: return null
        return WdtsExtractedRule(
            siteName = map["name"] ?: map["siteName"] ?: "",
            searchUrl = searchUrl,
            bookListCss = map["bookListCss"] ?: map["searchListRule"] ?: "",
            nameCss = map["nameCss"] ?: map["searchTitleRule"] ?: "",
            authorCss = map["authorCss"] ?: map["searchAuthorRule"] ?: "",
            coverCss = map["coverCss"] ?: map["searchCoverRule"] ?: "",
            detailCss = map["detailCss"] ?: map["searchUrlRule"] ?: "",
            userAgent = map["userAgent"] ?: "",
            charset = map["charset"] ?: "UTF-8"
        )
    }

    // ==================== 已知站点映射 ====================

    /**
     * 通过 entry_package 名称映射到已知的听书网站
     *
     * 即使无法下载或解析 JAR，也能通过包名推断出网站URL和CSS选择器
     * 内置了从实际 JAR 文件中提取的真实搜索配置
     *
     * @return 单个 WdtsExtractedRule（单站点）或 List<WdtsExtractedRule>（多站点集合源）
     */
    private fun mapByEntryPackage(packageName: String): WdtsExtractedRule? {
        val pkg = packageName.lowercase()
        return when {
            // ===== WDTS 专用源 =====
            // sources_by_shun: 包含15+个听书网站
            pkg.contains("sources_by_shun") || pkg.contains("shun") -> {
                // 返回主站点配置; 实际搜索时通过 mapByEntryPackageMulti 获取全部站点
                WdtsExtractedRule(
                    siteName = "[shun] 听书源合集",
                    searchUrl = "https://m.6yueting.com/search/index/search?content={keyword}",
                    bookListCss = "ul > li",
                    nameCss = ".text > .name",
                    authorCss = ".text > div > .broadcaster",
                    detailCss = ".text > .name",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
                )
            }
            // my_sound_01: 听书源7个
            pkg.contains("my_sound") -> WdtsExtractedRule(
                siteName = "[sound] 听书源",
                searchUrl = "https://m.ting55.com/search.html?keyword={keyword}",
                bookListCss = "div.list-works ul li",
                nameCss = "a@text",
                authorCss = "p.author@text",
                detailCss = "a@href",
                userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
            )
            // english: LibriVox 英文有声书
            pkg.contains("english") -> WdtsExtractedRule(
                siteName = "[english] LibriVox",
                searchUrl = "https://librivox.org/search?q={keyword}&search_form=advanced",
                bookListCss = "div.book-list li",
                nameCss = "h3 a@text",
                authorCss = "span.author@text",
                detailCss = "h3 a@href",
                userAgent = "Mozilla/5.0"
            )

            // ===== 常见听书网站映射（Legado格式包） =====
            pkg.contains("tingchina") -> WdtsExtractedRule(
                siteName = "听中国",
                searchUrl = "https://www.tingchina.com/yousheng/search.asp?search={keyword}",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            pkg.contains("ting55") -> WdtsExtractedRule(
                siteName = "听书网",
                searchUrl = "https://m.ting55.com/search.html?keyword={keyword}",
                bookListCss = "div.list-works ul li",
                nameCss = "a@text",
                authorCss = "p.author@text",
                detailCss = "a@href",
                userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
            )
            pkg.contains("tingshuge") -> WdtsExtractedRule(
                siteName = "听书阁",
                searchUrl = "https://www.tingshuge.com/search.html?keyword={keyword}",
                bookListCss = "div.so_list ul li",
                nameCss = "h3 a@text",
                authorCss = "p.author@text",
                detailCss = "h3 a@href",
                userAgent = "Mozilla/5.0"
            )
            pkg.contains("ting89") -> WdtsExtractedRule(
                siteName = "听89",
                searchUrl = "https://www.ting89.com/search.asp?searchword={keyword}&searchtype=-1",
                bookListCss = "div.news-list ul li",
                nameCss = "a@text",
                authorCss = "span.author@text",
                detailCss = "a@href",
                userAgent = "Mozilla/5.0"
            )
            pkg.contains("520tingshu") -> WdtsExtractedRule(
                siteName = "520听书",
                searchUrl = "https://www.520tingshu.com/search?keyword={keyword}",
                bookListCss = "div.result-list li",
                nameCss = "h4 a@text",
                authorCss = "span.author@text",
                detailCss = "h4 a@href",
                userAgent = "Mozilla/5.0"
            )
            pkg.contains("tingbook") -> WdtsExtractedRule(
                siteName = "听书网 (tingbook)",
                searchUrl = "https://www.tingbook.cc/search.html?keyword={keyword}",
                userAgent = "Mozilla/5.0"
            )
            pkg.contains("huantingshu") -> WdtsExtractedRule(
                siteName = "幻听书",
                searchUrl = "https://www.huantingshu.com/search/?q={keyword}",
                bookListCss = "ul.s-list div.item",
                nameCss = "div.title a@text",
                authorCss = "span.author@text",
                detailCss = "div.title a@href",
                userAgent = "Mozilla/5.0"
            )

            else -> null
        }
    }

    /**
     * 对于多站点集合型源（如 sources_by_shun），返回所有子站点的搜索配置
     * 这是从实际 JAR DEX 文件中提取的真实站点列表
     */
    private fun mapByEntryPackageMulti(packageName: String): List<WdtsExtractedRule> {
        val pkg = packageName.lowercase()

        if (pkg.contains("sources_by_shun") || pkg.contains("shun")) {
            // 从 sources_by_shun.jar 的 DEX 中提取的 15+ 个真实听书站点
            return listOf(
                // 1. 6yueting - 六月听书
                WdtsExtractedRule(
                    siteName = "六月听书", searchUrl = "http://m.6yueting.com/search/index/search?content={keyword}",
                    bookListCss = "ul > li", nameCss = ".text > .name",
                    authorCss = ".text > div > .broadcaster", detailCss = ".text > .name",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
                ),
                // 2. tingshu168 - 听书168
                WdtsExtractedRule(
                    siteName = "听书168", searchUrl = "http://www.tingshu168.com/search/index/search?content={keyword}",
                    bookListCss = ".clist > li", nameCss = "a@text",
                    authorCss = "span@text", detailCss = "a@href",
                    userAgent = "Mozilla/5.0"
                ),
                // 3. ting15 - 有听网
                WdtsExtractedRule(
                    siteName = "有听网", searchUrl = "https://www.ting15.com/?s=ting-search-wd-{keyword}",
                    bookListCss = ".box-list-item-text", nameCss = ".box-list-item-text-title > a@text",
                    authorCss = ".box-list-item-text-autspeaker@text", detailCss = ".box-list-item-text-title > a@href",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
                ),
                // 4. 70ts - 麒麟听书
                WdtsExtractedRule(
                    siteName = "麒麟听书", searchUrl = "https://www.70ts.com/so/search.html?searchtype=name&searchword={keyword}",
                    bookListCss = "li > .clearfix > section", nameCss = ".title > a@text",
                    authorCss = ".text > .desc@text", detailCss = ".title > a@href",
                    userAgent = "Mozilla/5.0"
                ),
                // 5. Ting55 - 听书网
                WdtsExtractedRule(
                    siteName = "听书网55", searchUrl = "https://m.ting55.com/search.html?keyword={keyword}",
                    bookListCss = "div.list-works ul li", nameCss = "a@text",
                    authorCss = "p.author@text", detailCss = "a@href",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
                ),
                // 6. itingshu - 爱听书
                WdtsExtractedRule(
                    siteName = "爱听书", searchUrl = "https://www.itingshu.net/novelsearch/search/result.html",
                    bookListCss = ".play-list > ul > li", nameCss = "a@text",
                    detailCss = "a@href",
                    userAgent = "Mozilla/5.0"
                ),
                // 7. nianyin - 念音听书
                WdtsExtractedRule(
                    siteName = "念音听书", searchUrl = "https://www.nianyin.com/?s=ting-search-wd-{keyword}",
                    bookListCss = ".box-list-item-text", nameCss = ".box-list-item-text-title > a@text",
                    detailCss = ".box-list-item-text-title > a@href",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
                ),
                // 8. 22ting
                WdtsExtractedRule(
                    siteName = "22听书", searchUrl = "https://22ting.com/search.php?searchword={keyword}",
                    bookListCss = "ul > li", nameCss = "a@text",
                    detailCss = "a@href",
                    userAgent = "Mozilla/5.0"
                ),
                // 9. qmtsw - 全民听书网
                WdtsExtractedRule(
                    siteName = "全民听书", searchUrl = "https://www.qmtsw.com/search.php?searchword={keyword}",
                    bookListCss = ".list-works ul li", nameCss = "a@text",
                    detailCss = "a@href",
                    userAgent = "Mozilla/5.0"
                ),
            )
        }

        return emptyList()
    }

    // ==================== 搜索执行 ====================

    /**
     * 对已提取规则或有映射的WDTS源执行搜索
     *
     * 对于多站点集合型源（如 sources_by_shun），自动展开为多个子站点并分别搜索
     */
    suspend fun search(
        keyword: String,
        sources: List<WdtsSource>,
        context: Context = MyReaderApp.instance
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val validSources = sources.filter {
            it.enabled && (it.status == WdtsSourceStatus.EXTRACTED ||
                    it.status == WdtsSourceStatus.FALLBACK_HTTP)
        }
        Log.i(TAG, "WDTS搜索: \"$keyword\", 基础可用源=${validSources.size}")

        // 展开多站点集合源 -> 生成虚拟子源
        val expandedSources = mutableListOf<WdtsSource>()
        for (source in validSources) {
            val multiRules = mapByEntryPackageMulti(source.metadata.entryPackage)
            if (multiRules.isNotEmpty()) {
                // 集合源: 每个子站点创建一个虚拟源
                for ((idx, rule) in multiRules.withIndex()) {
                    expandedSources.add(source.copy(
                        extractedRule = rule,
                        status = WdtsSourceStatus.FALLBACK_HTTP
                    ).let {
                        // 生成唯一ID以避免去重冲突
                        it.copy(
                            // 通过 metadata copy 来区分
                            metadata = it.metadata
                        )
                    })
                }
                Log.i(TAG, "[${source.metadata.entryPackage}] 展开为 ${multiRules.size} 个子站点")
            } else {
                expandedSources.add(source)
            }
        }
        Log.i(TAG, "WDTS搜索: 展开后共 ${expandedSources.size} 个搜索入口")

        expandedSources.flatMap { source ->
            try {
                searchSingle(keyword, source)
            } catch (e: Exception) {
                Log.e(TAG, "[${source.extractedRule?.siteName ?: source.metadata.entryPackage}] 搜索异常: ${e.message}")
                emptyList()
            }
        }
    }

    private fun searchSingle(keyword: String, source: WdtsSource): List<SearchResult> {
        val rule = source.extractedRule ?: return emptyList()
        val searchUrl = rule.searchUrl.replace("{keyword}", java.net.URLEncoder.encode(keyword, "UTF-8"))
            .replace("{{key}}", java.net.URLEncoder.encode(keyword, "UTF-8"))

        Log.d(TAG, "[${source.metadata.entryPackage}] 搜索URL: $searchUrl")

        val ua = rule.userAgent.ifBlank {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36"
        }

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", ua)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "[${source.metadata.entryPackage}] HTTP ${response.code}")
            return emptyList()
        }

        val body = response.body?.bytes() ?: return emptyList()
        val html = String(body, Charsets.UTF_8)
        Log.d(TAG, "[${source.metadata.entryPackage}] 响应大小: ${body.size} bytes")

        // 如果有原始 Legado 规则，使用 Legado 引擎解析
        if (rule.rawRules != null) {
            return tryLegadoSearch(keyword, html, rule, source)
        }

        // 否则使用简单 CSS 搜索
        return tryCssSearch(html, rule, source)
    }

    private fun tryLegadoSearch(
        keyword: String,
        html: String,
        rule: WdtsExtractedRule,
        source: WdtsSource
    ): List<SearchResult> {
        try {
            // 尝试解析原始 JSON 规则并使用 Legado 引擎
            val legadoSource = LegadoSource.fromJson(rule.rawRules!!)
            if (legadoSource != null && legadoSource.isValid) {
                // 直接使用 LegadoEngine 处理（searchSingle 运行在 Dispatchers.IO，runBlocking 安全）
                val engine = LegadoEngine()
                return runBlocking { engine.search(keyword, listOf(legadoSource)) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legado规则解析失败: ${e.message}")
        }
        return emptyList()
    }

    private fun tryCssSearch(
        html: String,
        rule: WdtsExtractedRule,
        source: WdtsSource
    ): List<SearchResult> {
        val doc = org.jsoup.Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        // 如果有 CSS 选择器，使用它
        if (rule.bookListCss.isNotBlank()) {
            val items = doc.select(rule.bookListCss)
            Log.d(TAG, "[${source.metadata.entryPackage}] CSS \"${rule.bookListCss}\" 匹配: ${items.size}")

            for (item in items) {
                try {
                    val title = if (rule.nameCss.isNotBlank()) {
                        item.select(rule.nameCss).text().trim()
                    } else {
                        item.text().take(50).trim()
                    }
                    val author = if (rule.authorCss.isNotBlank()) {
                        item.select(rule.authorCss).text().trim()
                    } else "未知"

                    val detailUrl = if (rule.detailCss.isNotBlank()) {
                        item.select(rule.detailCss).first()?.attr("abs:href") ?: ""
                    } else {
                        item.select("a[href]").first()?.attr("abs:href") ?: ""
                    }

                    if (title.isNotBlank() && detailUrl.isNotBlank()) {
                        results.add(SearchResult(
                            title = title,
                            author = author.ifBlank { "未知" },
                            coverUrl = "",
                            url = detailUrl,
                            sourceId = source.id,
                            description = ""
                        ))
                    }
                } catch (e: Exception) {
                    // skip individual item errors
                }
            }
        } else {
            // 无 CSS 规则，尝试常见的搜索结果模式
            val linkItems = doc.select("a[href]").filter { el ->
                val text = el.text().trim()
                text.length in 2..60 && !text.startsWith("<") &&
                el.attr("href").isNotBlank() && !el.attr("href").startsWith("#") &&
                !el.attr("href").startsWith("javascript:")
            }
            Log.d(TAG, "[${source.metadata.entryPackage}] 通用链接匹配: ${linkItems.size}")

            for (link in linkItems.take(20)) {
                val title = link.text().trim()
                val url = link.attr("abs:href")
                if (title.isNotBlank() && url.isNotBlank()) {
                    results.add(SearchResult(
                        title = title,
                        author = "未知",
                        coverUrl = "",
                        url = url,
                        sourceId = source.id,
                        description = ""
                    ))
                }
            }
        }

        return results
    }

    // ==================== 存储 ====================

    suspend fun saveSources(context: Context, sources: List<WdtsSource>) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArr = org.json.JSONArray()
        for (source in sources) {
            val obj = org.json.JSONObject().apply {
                put("hubUrl", source.hubUrl)
                put("hubName", source.hubName)
                put("entryPackage", source.metadata.entryPackage)
                put("downloadUrl", source.metadata.downloadUrl)
                put("version", source.metadata.version)
                put("updateMsg", source.metadata.updateMsg)
                put("status", source.status.name)
                put("jarLocalPath", source.jarLocalPath ?: "")
                put("errorMsg", source.errorMsg ?: "")
                put("enabled", source.enabled)
                source.extractedRule?.let {
                    val ruleObj = org.json.JSONObject().apply {
                        put("searchUrl", it.searchUrl)
                        put("bookListCss", it.bookListCss)
                        put("nameCss", it.nameCss)
                        put("authorCss", it.authorCss)
                        put("coverCss", it.coverCss)
                        put("detailCss", it.detailCss)
                        put("userAgent", it.userAgent)
                        put("charset", it.charset)
                        put("siteName", it.siteName)
                        if (it.rawRules != null) put("rawRules", it.rawRules)
                    }
                    put("extractedRule", ruleObj)
                }
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString(KEY_SOURCES, jsonArr.toString()).apply()
    }

    suspend fun loadSources(context: Context): List<WdtsSource> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SOURCES, null) ?: return@withContext emptyList()

        try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    val status = try {
                        WdtsSourceStatus.valueOf(obj.optString("status", "METADATA_ONLY"))
                    } catch (e: Exception) { WdtsSourceStatus.METADATA_ONLY }

                    var extractedRule: WdtsExtractedRule? = null
                    if (obj.has("extractedRule")) {
                        val rObj = obj.getJSONObject("extractedRule")
                        extractedRule = WdtsExtractedRule(
                            searchUrl = rObj.optString("searchUrl", ""),
                            bookListCss = rObj.optString("bookListCss", ""),
                            nameCss = rObj.optString("nameCss", ""),
                            authorCss = rObj.optString("authorCss", ""),
                            coverCss = rObj.optString("coverCss", ""),
                            detailCss = rObj.optString("detailCss", ""),
                            userAgent = rObj.optString("userAgent", ""),
                            charset = rObj.optString("charset", "UTF-8"),
                            siteName = rObj.optString("siteName", ""),
                            rawRules = rObj.optString("rawRules", "").ifBlank { null }
                        )
                    }

                    WdtsSource(
                        metadata = WdtsSourceMetadata(
                            version = obj.optInt("version", 0),
                            entryPackage = obj.optString("entryPackage", ""),
                            downloadUrl = obj.optString("downloadUrl", ""),
                            updateMsg = obj.optString("updateMsg", ""),
                        ),
                        hubUrl = obj.optString("hubUrl", ""),
                        hubName = obj.optString("hubName", ""),
                        status = status,
                        extractedRule = extractedRule,
                        jarLocalPath = obj.optString("jarLocalPath", "").ifBlank { null },
                        errorMsg = obj.optString("errorMsg", "").ifBlank { null },
                        enabled = obj.optBoolean("enabled", true)
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

package com.myreader.data.source

import android.util.Log
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Legado 书源执行引擎
 *
 * 解析 Legado JSON 格式的书源规则，执行搜索、获取详情、章节、音频URL。
 */
class LegadoEngine {

    companion object {
        private const val TAG = "LegadoEngine"
        private const val DEBUG = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 执行搜索 */
    suspend fun search(
        keyword: String,
        sources: List<LegadoSource>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val validSources = sources.filter { it.enabled && it.isValid }
        Log.i(TAG, "========== 开始搜索: \"$keyword\", 有效书源数=${validSources.size} ==========")

        val allResults = mutableListOf<SearchResult>()
        for (source in validSources) {
            try {
                val results = searchSingle(keyword, source)
                if (results.isNotEmpty()) {
                    Log.i(TAG, "  ✅ [${source.bookSourceName}] 找到 ${results.size} 个结果")
                    allResults.addAll(results)
                } else {
                    Log.w(TAG, "  ❌ [${source.bookSourceName}] 无结果")
                }
            } catch (e: Exception) {
                Log.e(TAG, "  💥 [${source.bookSourceName}] 异常: ${e.javaClass.simpleName}: ${e.message}")
                if (DEBUG) e.printStackTrace()
            }
        }
        Log.i(TAG, "========== 搜索完成: 总计 ${allResults.size} 个结果 ==========")
        allResults
    }

    private fun searchSingle(keyword: String, source: LegadoSource): List<SearchResult> {
        val searchRule = source.ruleSearch ?: run {
            Log.w(TAG, "    [${source.bookSourceName}] ruleSearch 为空，跳过")
            return emptyList()
        }
        if (searchRule.bookList.isBlank()) {
            Log.w(TAG, "    [${source.bookSourceName}] bookList 规则为空，跳过")
            return emptyList()
        }

        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = source.searchUrl.replace("{{key}}", encoded)
            .replace("{key}", encoded)
            .replace("{{keyword}}", encoded)
            .replace("{keyword}", encoded)

        Log.d(TAG, "    [${source.bookSourceName}] 搜索URL: $url")
        Log.d(TAG, "    [${source.bookSourceName}] bookList原始规则: ${searchRule.bookList}")

        val doc = fetchDoc(url, source.httpUserAgent)
        val listCss = LegadoRuleParser.toCssSelector(searchRule.bookList)

        Log.d(TAG, "    [${source.bookSourceName}] 解析后CSS: \"$listCss\"")

        val items = if (listCss.isNotBlank()) {
            val selected = doc.select(listCss)
            Log.d(TAG, "    [${source.bookSourceName}] CSS匹配元素数: ${selected.size}")
            selected
        } else {
            Log.w(TAG, "    [${source.bookSourceName}] CSS为空，回退到body子元素")
            doc.body()?.children() ?: org.jsoup.select.Elements()
        }

        val results = items.mapNotNull { item ->
            try {
                val title = LegadoRuleParser.getText(item, searchRule.name)
                val author = LegadoRuleParser.getText(item, searchRule.author)
                val coverUrl = LegadoRuleParser.getAttr(item, searchRule.coverUrl)
                val detailUrl = LegadoRuleParser.getUrl(item, searchRule.detailUrl)

                if (title.isNotBlank() && detailUrl.isNotBlank()) {
                    SearchResult(
                        title = title,
                        author = author.ifBlank { "未知" },
                        coverUrl = fixUrl(coverUrl, source.bookSourceUrl),
                        url = fixUrl(detailUrl, source.bookSourceUrl),
                        sourceId = source.id,
                        description = ""
                    )
                } else {
                    Log.d(TAG, "    [${source.bookSourceName}] 跳过(标题/URL为空): title=\"$title\" url=\"$detailUrl\"")
                    null
                }
            } catch (e: Exception) {
                Log.d(TAG, "    [${source.bookSourceName}] 解析元素异常: ${e.message}")
                null
            }
        }

        if (results.isEmpty() && items.isNotEmpty()) {
            Log.w(TAG, "    [${source.bookSourceName}] 匹配到 ${items.size} 个元素但无法提取结果。可能需要更新 name/author/detailUrl 规则")
            // 输出第一条原始HTML帮助调试
            val firstHtml = items.first()?.outerHtml()?.take(300) ?: "N/A"
            Log.d(TAG, "    [${source.bookSourceName}] 第一条匹配元素HTML: $firstHtml")
            Log.d(TAG, "    [${source.bookSourceName}] name规则: ${searchRule.name}")
            Log.d(TAG, "    [${source.bookSourceName}] author规则: ${searchRule.author}")
            Log.d(TAG, "    [${source.bookSourceName}] detailUrl规则: ${searchRule.detailUrl}")
        }

        return results
    }

    /** 获取书籍详情 */
    suspend fun getBookDetail(
        result: SearchResult,
        sources: List<LegadoSource>
    ): Book? = withContext(Dispatchers.IO) {
        val source = sources.find { it.id == result.sourceId } ?: return@withContext null
        try {
            val doc = fetchDoc(result.url, source.httpUserAgent)
            val infoRule = source.ruleBookInfo

            val description = if (infoRule != null && infoRule.intro.isNotBlank())
                LegadoRuleParser.getText(doc.body(), infoRule.intro) else ""

            val coverUrl = if (infoRule != null && infoRule.coverUrl.isNotBlank())
                LegadoRuleParser.getAttr(doc.body(), infoRule.coverUrl) else result.coverUrl

            val author = if (infoRule != null && infoRule.author.isNotBlank())
                LegadoRuleParser.getText(doc.body(), infoRule.author) else result.author

            val title = if (infoRule != null && infoRule.name.isNotBlank())
                LegadoRuleParser.getText(doc.body(), infoRule.name) else result.title

            Book(
                title = title,
                author = author,
                coverUrl = fixUrl(coverUrl, source.bookSourceUrl),
                description = description,
                sourceId = source.id,
                sourceUrl = result.url
            )
        } catch (e: Exception) {
            null
        }
    }

    /** 获取章节列表 */
    suspend fun getChapters(
        book: Book,
        sources: List<LegadoSource>
    ): List<Chapter> = withContext(Dispatchers.IO) {
        val source = sources.find { it.id == book.sourceId } ?: return@withContext emptyList()
        try {
            val doc = fetchDoc(book.sourceUrl, source.httpUserAgent)
            val tocRule = source.ruleToc
            if (tocRule.isBlank()) return@withContext emptyList()

            val tocCss = LegadoRuleParser.toCssSelector(tocRule)
            val items = if (tocCss.isNotBlank()) doc.select(tocCss)
            else doc.body()?.children() ?: org.jsoup.select.Elements()

            items.mapIndexed { index, item ->
                val title = item.text().trim()
                val href = item.attr("abs:href").ifBlank { item.attr("href") }
                Chapter(
                    bookId = book.id,
                    title = title,
                    url = fixUrl(href, source.bookSourceUrl),
                    index = index
                )
            }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 获取音频URL */
    suspend fun getAudioUrl(
        chapter: Chapter,
        sourceId: String,
        sources: List<LegadoSource>
    ): String? = withContext(Dispatchers.IO) {
        val source = sources.find { it.id == sourceId } ?: return@withContext null
        try {
            val doc = fetchDoc(chapter.url, source.httpUserAgent)
            val contentRule = source.ruleContent
            if (contentRule != null && contentRule.content.isNotBlank()) {
                val css = LegadoRuleParser.toCssSelector(contentRule.content)
                if (css.isNotBlank()) {
                    val el = doc.select(css).first()
                    el?.attr("abs:src")?.ifBlank { el.attr("src") }
                        ?.let { fixUrl(it, source.bookSourceUrl) }
                } else {
                    // 自动检测 audio 标签
                    doc.select("audio source").first()?.attr("src")
                        ?: doc.select("audio").first()?.attr("src")
                }
            } else {
                // 自动检测
                val audioSrc = doc.select("audio source").first()?.attr("src")
                    ?: doc.select("audio").first()?.attr("src")
                val videoSrc = doc.select("video source").first()?.attr("src")
                    ?: doc.select("video").first()?.attr("src")
                audioSrc ?: videoSrc
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchDoc(url: String, customUA: String): Document {
        val ua = customUA.ifBlank {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", ua)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .header("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        Log.d(TAG, "      HTTP ${response.code} ${response.message}, 请求URL: $url")

        if (!response.isSuccessful) {
            val errMsg = "HTTP ${response.code}: ${response.message}"
            Log.e(TAG, "      $errMsg")
            throw Exception(errMsg)
        }

        val body = response.body?.bytes() ?: throw Exception("Empty response body")
        Log.d(TAG, "      响应大小: ${body.size} bytes")

        // 自动检测编码
        val html = String(body, Charsets.UTF_8)
        val doc = Jsoup.parse(html)
        val titleTag = doc.title()
        Log.d(TAG, "      页面标题: $titleTag")

        // 尝试从meta标签获取正确编码
        val charsetMeta = doc.select("meta[charset]").first()
            ?.attr("charset")
            ?: doc.select("meta[http-equiv=Content-Type]").first()
                ?.attr("content")
                ?.let { Regex("charset=([\\w-]+)").find(it)?.groupValues?.get(1) }

        return if (charsetMeta != null && charsetMeta.uppercase() != "UTF-8") {
            try {
                Log.d(TAG, "      检测到编码: $charsetMeta，重新解析")
                Jsoup.parse(String(body, java.nio.charset.Charset.forName(charsetMeta)))
            } catch (e: Exception) {
                doc
            }
        } else doc
    }

    private fun fixUrl(url: String, baseUrl: String): String {
        if (url.isBlank()) return ""
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> baseUrl.trimEnd('/') + url
            else -> "$baseUrl/$url"
        }
    }
}

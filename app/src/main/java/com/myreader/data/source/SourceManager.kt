package com.myreader.data.source

import android.content.Context
import android.content.SharedPreferences
import com.myreader.MyReaderApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 书源管理器
 *
 * 负责书源的持久化存储和增删改查。
 * 使用 SharedPreferences 存储 JSON 数组。
 *
 * 管理三类书源：
 * 1. Legado JSON 源（阅读格式）
 * 2. CSS 规则源（内置）
 * 3. WDTS（我的听书）JAR 源
 */
object SourceManager {

    private const val PREFS_NAME = "book_sources"
    private const val KEY_SOURCES = "legado_sources"
    private const val KEY_CSS_SOURCES = "css_sources"
    private const val KEY_PREBUILT_LOADED = "prebuilt_loaded_v2"
    private const val KEY_WDTS_INITIALIZED = "wdts_initialized_v1"

    private val _sources = MutableStateFlow<List<LegadoSource>>(emptyList())
    val sources: StateFlow<List<LegadoSource>> = _sources.asStateFlow()

    private val _cssSources = MutableStateFlow<List<SourceRule>>(emptyList())
    val cssSources: StateFlow<List<SourceRule>> = _cssSources.asStateFlow()

    private val _wdtsSources = MutableStateFlow<List<WdtsSource>>(emptyList())
    val wdtsSources: StateFlow<List<WdtsSource>> = _wdtsSources.asStateFlow()

    /** 所有可用源（Legado + CSS + WDTS）的总数 */
    val allSourcesCount: Int
        get() = _sources.value.count { it.enabled } +
                _cssSources.value.count { it.enabled } +
                _wdtsSources.value.count { it.enabled && it.isValidForSearch }

    /** 初始化：从存储加载书源 */
    suspend fun init(context: Context = MyReaderApp.instance) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 加载 Legado 源
        val json = prefs.getString(KEY_SOURCES, null)
        val legadoSources = if (json != null) {
            try {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { i ->
                    try {
                        LegadoSource.fromJsonObject(arr.getJSONObject(i))
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) { emptyList() }
        } else emptyList()

        // 首次启动：自动加载预置有声书源
        val prebuiltLoaded = prefs.getBoolean(KEY_PREBUILT_LOADED, false)
        if (legadoSources.isEmpty() || !prebuiltLoaded) {
            try {
                val prebuilt = PrebuiltAudiobookSources.allSources()
                _sources.value = prebuilt
                saveSources()
                prefs.edit().putBoolean(KEY_PREBUILT_LOADED, true).apply()
            } catch (e: Exception) {
                _sources.value = legadoSources
            }
        } else {
            _sources.value = legadoSources
        }

        // 加载 CSS 源（内置源作为后备，但优先使用 Legado 源搜索）
        _cssSources.value = BuiltinSources.ALL

        // 加载 WDTS 源
        _wdtsSources.value = WdtsSourceClient.loadSources(context)

        // 扫描本地导入的 JAR 文件（离线源）
        val localJars = WdtsSourceClient.discoverLocalJars(context)
        if (localJars.isNotEmpty()) {
            val existingIds = _wdtsSources.value.map { it.id }.toSet()
            val localSources = localJars.map { (name, path) ->
                WdtsSourceClient.createSourceFromLocalJar(name, path, "本地导入")
            }.filter { it.id !in existingIds }

            if (localSources.isNotEmpty()) {
                // 自动提取本地JAR的配置
                val extractedSources = localSources.map { src ->
                    try {
                        WdtsSourceClient.extractRules(src)
                    } catch (e: Exception) {
                        src
                    }
                }
                _wdtsSources.value = _wdtsSources.value + extractedSources
                WdtsSourceClient.saveSources(context, _wdtsSources.value)
            }
        }

        // 首次初始化：自动获取 WDTS 元数据
        val wdtsInit = prefs.getBoolean(KEY_WDTS_INITIALIZED, false)
        if (!wdtsInit) {
            try {
                val wdtsList = WdtsSourceClient.fetchAllMetadata()
                if (wdtsList.isNotEmpty()) {
                    _wdtsSources.value = mergeWdtsSources(_wdtsSources.value, wdtsList)
                    WdtsSourceClient.saveSources(context, _wdtsSources.value)
                }
                prefs.edit().putBoolean(KEY_WDTS_INITIALIZED, true).apply()
            } catch (e: Exception) {
                // 首次获取失败不阻塞启动
            }
        }
    }

    /** 重新加载预置有声书源（覆盖） */
    suspend fun reloadPrebuilt() = withContext(Dispatchers.IO) {
        try {
            val prebuilt = PrebuiltAudiobookSources.allSources()
            _sources.value = prebuilt
            saveSources()
            val prefs = MyReaderApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PREBUILT_LOADED, true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 获取启用的 Legado 源 */
    fun getEnabled(): List<LegadoSource> = _sources.value.filter { it.enabled }

    /** 获取启用的 CSS 源 */
    fun getEnabledCss(): List<SourceRule> = _cssSources.value.filter { it.enabled }

    /** 添加书源 */
    suspend fun addSource(source: LegadoSource) = withContext(Dispatchers.IO) {
        val current = _sources.value.toMutableList()
        // 避免重复（按URL去重）
        if (current.none { it.bookSourceUrl == source.bookSourceUrl }) {
            current.add(source)
            _sources.value = current
            saveSources()
        }
    }

    /** 批量添加书源 */
    suspend fun addSources(sources: List<LegadoSource>) = withContext(Dispatchers.IO) {
        val current = _sources.value.toMutableList()
        val existingUrls = current.map { it.bookSourceUrl }.toSet()
        val newSources = sources.filter { it.isValid && it.bookSourceUrl !in existingUrls }
        if (newSources.isNotEmpty()) {
            current.addAll(newSources)
            _sources.value = current
            saveSources()
        }
    }

    /** 删除书源 */
    suspend fun removeSource(source: LegadoSource) = withContext(Dispatchers.IO) {
        _sources.value = _sources.value.filter { it.bookSourceUrl != source.bookSourceUrl }
        saveSources()
    }

    /** 更新书源启用状态 */
    suspend fun toggleSource(source: LegadoSource) = withContext(Dispatchers.IO) {
        _sources.value = _sources.value.map {
            if (it.bookSourceUrl == source.bookSourceUrl) it.copy(enabled = !it.enabled)
            else it
        }
        saveSources()
    }

    /** 替换全部书源 */
    suspend fun replaceAll(sources: List<LegadoSource>) = withContext(Dispatchers.IO) {
        _sources.value = sources.filter { it.isValid }
        saveSources()
    }

    // ==================== WDTS 源管理 ====================

    /** 获取启用的 WDTS 源 */
    fun getEnabledWdts(): List<WdtsSource> =
        _wdtsSources.value.filter { it.enabled && it.isValidForSearch }

    /** 刷新 WDTS 源元数据 */
    suspend fun refreshWdtsMetadata(progress: ((String) -> Unit)? = null) =
        withContext(Dispatchers.IO) {
            val newSources = WdtsSourceClient.fetchAllMetadata(progress)
            // 合并：保留已下载的状态
            val merged = mergeWdtsSources(_wdtsSources.value, newSources)
            _wdtsSources.value = merged
            WdtsSourceClient.saveSources(MyReaderApp.instance, merged)
        }

    /** 下载并提取 WDTS 源的 JAR */
    suspend fun downloadWdtsJar(
        source: WdtsSource,
        progress: ((String) -> Unit)? = null
    ): WdtsSource = withContext(Dispatchers.IO) {
        // 下载
        val downloaded = WdtsSourceClient.downloadJar(source, progress = progress)
        // 提取
        val extracted = WdtsSourceClient.extractRules(downloaded)
        // 更新列表
        _wdtsSources.value = _wdtsSources.value.map {
            if (it.id == extracted.id) extracted else it
        }
        WdtsSourceClient.saveSources(MyReaderApp.instance, _wdtsSources.value)
        extracted
    }

    /** 下载所有 WDTS 源的 JAR */
    suspend fun downloadAllWdtsJars(
        progress: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val updated = mutableListOf<WdtsSource>()
        for ((i, source) in _wdtsSources.value.withIndex()) {
            progress?.invoke("处理中 (${i + 1}/${_wdtsSources.value.size}): ${source.metadata.entryPackage}...")
            val downloaded = WdtsSourceClient.downloadJar(source, progress = progress)
            val extracted = WdtsSourceClient.extractRules(downloaded)
            updated.add(extracted)
        }
        _wdtsSources.value = updated
        WdtsSourceClient.saveSources(MyReaderApp.instance, updated)
    }

    /** 切换 WDTS 源启用状态 */
    suspend fun toggleWdtsSource(source: WdtsSource) = withContext(Dispatchers.IO) {
        _wdtsSources.value = _wdtsSources.value.map {
            if (it.id == source.id) it.copy(enabled = !it.enabled) else it
        }
        WdtsSourceClient.saveSources(MyReaderApp.instance, _wdtsSources.value)
    }

    /** 删除 WDTS 源 */
    suspend fun removeWdtsSource(source: WdtsSource) = withContext(Dispatchers.IO) {
        _wdtsSources.value = _wdtsSources.value.filter { it.id != source.id }
        // 也删除本地 JAR 文件
        source.jarLocalPath?.let { File(it).delete() }
        WdtsSourceClient.saveSources(MyReaderApp.instance, _wdtsSources.value)
    }

    /** 清空 WDTS 源 */
    suspend fun clearWdtsAll() = withContext(Dispatchers.IO) {
        _wdtsSources.value = emptyList()
        val prefs = MyReaderApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_WDTS_INITIALIZED).apply()
        WdtsSourceClient.saveSources(MyReaderApp.instance, emptyList())
    }

    private fun mergeWdtsSources(
        existing: List<WdtsSource>,
        newOnes: List<WdtsSource>
    ): List<WdtsSource> {
        val existingMap = existing.associateBy { it.id }
        return newOnes.map { new ->
            val old = existingMap[new.id]
            if (old != null && old.status != WdtsSourceStatus.METADATA_ONLY) {
                // 保留已下载/提取的状态
                old.copy(metadata = new.metadata)
            } else {
                new
            }
        }
    }

    /** 清空全部书源 */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        _sources.value = emptyList()
        val prefs = MyReaderApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SOURCES).apply()
    }

    /** 重置为示例源 */
    suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        _sources.value = emptyList()
        saveSources()
    }

    private fun saveSources() {
        val arr = JSONArray()
        _sources.value.forEach { source ->
            val obj = JSONObject().apply {
                put("bookSourceUrl", source.bookSourceUrl)
                put("bookSourceName", source.bookSourceName)
                put("bookSourceGroup", source.bookSourceGroup)
                put("searchUrl", source.searchUrl)
                put("bookSourceType", source.bookSourceType)
                put("httpUserAgent", source.httpUserAgent)
                put("ruleToc", source.ruleToc)
                put("enabled", source.enabled)
                source.ruleSearch?.let {
                    put("ruleSearch", JSONObject().apply {
                        put("bookList", it.bookList)
                        put("name", it.name)
                        put("author", it.author)
                        put("coverUrl", it.coverUrl)
                        put("detailUrl", it.detailUrl)
                        put("kind", it.kind)
                    })
                }
                source.ruleBookInfo?.let {
                    put("ruleBookInfo", JSONObject().apply {
                        put("name", it.name)
                        put("author", it.author)
                        put("coverUrl", it.coverUrl)
                        put("intro", it.intro)
                        put("kind", it.kind)
                    })
                }
                source.ruleContent?.let {
                    put("ruleContent", JSONObject().apply {
                        put("content", it.content)
                    })
                }
            }
            arr.put(obj)
        }
        val prefs = MyReaderApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SOURCES, arr.toString()).apply()
    }
}

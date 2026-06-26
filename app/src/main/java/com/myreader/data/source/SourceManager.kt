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

/**
 * 书源管理器
 *
 * 负责书源的持久化存储和增删改查。
 * 使用 SharedPreferences 存储 JSON 数组。
 */
object SourceManager {

    private const val PREFS_NAME = "book_sources"
    private const val KEY_SOURCES = "legado_sources"
    private const val KEY_CSS_SOURCES = "css_sources"
    private const val KEY_PREBUILT_LOADED = "prebuilt_loaded_v2"

    private val _sources = MutableStateFlow<List<LegadoSource>>(emptyList())
    val sources: StateFlow<List<LegadoSource>> = _sources.asStateFlow()

    private val _cssSources = MutableStateFlow<List<SourceRule>>(emptyList())
    val cssSources: StateFlow<List<SourceRule>> = _cssSources.asStateFlow()

    /** 所有可用源（Legado + CSS）的总数 */
    val allSourcesCount: Int
        get() = _sources.value.count { it.enabled } + _cssSources.value.count { it.enabled }

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

        // 加载 CSS 源（始终使用内置源作为后备）
        _cssSources.value = BuiltinSources.ALL
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

    /** 清空书源 */
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

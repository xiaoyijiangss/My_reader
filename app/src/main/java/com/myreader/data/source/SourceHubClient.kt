package com.myreader.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 书源社区客户端
 *
 * 从开源社区仓库获取书源，支持:
 * - GitHub Raw 直链
 * - 第三方镜像加速
 * - 本地 JSON 文件
 */
object SourceHubClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * 预配置的书源仓库
     * 这些是「阅读」APP 社区维护的公开书源合集
     */
    data class SourceHub(
        val name: String,
        val url: String,
        val description: String = ""
    )

    val KNOWN_HUBS = listOf(
        SourceHub(
            name = "XIUA2 阅读书源合集",
            url = "https://raw.githubusercontent.com/XIU2/Yuedu/master/shuyuan",
            description = "最全面的阅读书源集合，包含大量有声源"
        ),
        SourceHub(
            name = "CNAD666 书源合集",
            url = "https://raw.githubusercontent.com/CNAD666/MyData/master/novel/bookSource.json",
            description = "高质量精选书源"
        ),
        SourceHub(
            name = "Namo的书源合集",
            url = "https://raw.githubusercontent.com/Namo0O/legado_source/master/bookSource.json",
            description = "更新频繁的优质书源"
        ),
        SourceHub(
            name = "maotoumao书源 (含有声)",
            url = "https://raw.githubusercontent.com/maotoumao/MusicFreePlugins/master/plugins.json",
            description = "MusicFree 音频源插件合集"
        ),
    )

    /**
     * 从 URL 获取书源列表
     * @param url JSON 书源文件地址
     * @return 解析后的源列表
     */
    suspend fun fetchFromUrl(url: String): List<LegadoSource> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json,text/plain,*/*")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            if (body.isBlank()) return@withContext emptyList()

            // 尝试解析 JSON 数组
            val cleaned = body.trim()
            LegadoSource.fromJsonArray(cleaned)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 从预配置的仓库获取书源
     * @param hub 仓库配置
     * @return 解析后的源列表
     */
    suspend fun fetchFromHub(hub: SourceHub): List<LegadoSource> {
        return fetchFromUrl(hub.url)
    }

    /**
     * 从所有已知仓库获取书源
     * @return 所有源的合并列表（去重）
     */
    suspend fun fetchAllHubs(): List<LegadoSource> = withContext(Dispatchers.IO) {
        val allSources = mutableListOf<LegadoSource>()
        for (hub in KNOWN_HUBS) {
            try {
                val sources = fetchFromHub(hub)
                allSources.addAll(sources)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // 按 URL 去重
        allSources.distinctBy { it.bookSourceUrl }
    }

    /**
     * 从字符串导入源（用户粘贴的JSON）
     */
    fun importFromString(json: String): List<LegadoSource> {
        return LegadoSource.fromJsonArray(json.trim())
    }
}

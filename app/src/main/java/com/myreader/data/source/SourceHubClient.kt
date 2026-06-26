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
            name = "「我的听书」eprendre书源",
            url = "https://wdts.top/api/sources/external_sources.js",
            description = "eprendre制作，「我的听书」社区维护 (19源)"
        ),
        SourceHub(
            name = "「我的听书」shun书源",
            url = "https://wdts.top/api/sources/shun.json",
            description = "shun制作的热心书源 (9源)"
        ),
        SourceHub(
            name = "「我的听书」sound听书源",
            url = "https://wdts.top/api/sources/my_sound.json",
            description = "听书源合集，版本12 (7源)"
        ),
        SourceHub(
            name = "「我的听书」bxb100书源",
            url = "https://wdts.top/api/sources/bxb100.json",
            description = "bxb100制作的听书源"
        ),
        SourceHub(
            name = "「我的听书」英文有声源",
            url = "https://wdts.top/api/sources/english_source.json",
            description = "英文有声书源 (3源)"
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

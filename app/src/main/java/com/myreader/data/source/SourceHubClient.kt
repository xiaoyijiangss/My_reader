package com.myreader.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 书源社区客户端
 *
 * 收录「我的听书」社区和「阅读」社区的公开书源索引。
 * 
 * 注意：
 * - 「我的听书」(wdts.top) 的书源是 JAR 格式插件，仅供索引参考
 * - 「阅读」社区 (Legado) 的书源是 JSON 格式，可直接导入使用
 * - JAR 源需要「我的听书」APP 执行，本项目仅索引其 URL 供用户参考
 */
object SourceHubClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class SourceHub(
        val name: String,
        val url: String,
        val description: String = "",
        val type: HubType = HubType.LEGADO_JSON
    )

    enum class HubType { LEGADO_JSON, MY_TINGSHU_JAR }

    val KNOWN_HUBS = listOf(
        // ===== 「我的听书」社区书源索引 (JAR格式) =====
        SourceHub(
            name = "我的听书 — 外部书源 (19个)",
            url = "https://wdts.top/api/sources/external_sources.js",
            description = "eprendre制作，社区维护",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — shun书源 (9个)",
            url = "https://wdts.top/api/sources/shun.json",
            description = "shun制作的热心书源",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — sound听书源 (7个)",
            url = "https://wdts.top/api/sources/my_sound.json",
            description = "听书源合集 v12",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — bxb100书源",
            url = "https://wdts.top/api/sources/bxb100.json",
            description = "bxb100制作的听书源",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — 英文有声源 (3个)",
            url = "https://wdts.top/api/sources/english_source.json",
            description = "English audiobook sources",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — 视频源 (2个)",
            url = "https://wdts.top/api/sources/videosource.json",
            description = "视频资源源 v8",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — 网友视频源 (8个)",
            url = "https://wdts.top/api/sources/my_video.json",
            description = "热心网友制作的视频源 v7",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — 网友音乐源 (5个)",
            url = "https://wdts.top/api/sources/my_music.json",
            description = "热心网友制作的音乐源 v6",
            type = HubType.MY_TINGSHU_JAR
        ),
        SourceHub(
            name = "我的听书 — 助眠影音源",
            url = "https://wdts.top/api/sources/kyloasmr.json",
            description = "kyloasmr(老陈)维护的ASMR源 v32",
            type = HubType.MY_TINGSHU_JAR
        ),

        // ===== Legado「阅读」社区书源合集 =====
        SourceHub(
            name = "XIU2 阅读书源合集",
            url = "https://raw.githubusercontent.com/XIU2/Yuedu/master/shuyuan",
            description = "最全面的阅读书源集合 (含有声)",
            type = HubType.LEGADO_JSON
        ),
        SourceHub(
            name = "CNAD666 书源合集",
            url = "https://raw.githubusercontent.com/CNAD666/MyData/master/novel/bookSource.json",
            description = "高质量精选书源",
            type = HubType.LEGADO_JSON
        ),
        SourceHub(
            name = "Namo书源合集",
            url = "https://raw.githubusercontent.com/Namo0O/legado_source/master/bookSource.json",
            description = "更新频繁的优质书源",
            type = HubType.LEGADO_JSON
        ),
    )

    /**
     * 从 URL 获取书源列表 (仅 LEGADO_JSON 类型可用)
     */
    suspend fun fetchFromUrl(url: String): List<LegadoSource> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json,text/plain,*/*")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            if (body.isBlank()) return@withContext emptyList()
            val cleaned = body.trim()

            // 跳过「我的听书」JAR索引格式（不是Legado源）
            if (cleaned.startsWith("{") && cleaned.contains("\"download_url\"")) {
                return@withContext emptyList()
            }

            LegadoSource.fromJsonArray(cleaned)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchFromHub(hub: SourceHub): List<LegadoSource> {
        if (hub.type == HubType.MY_TINGSHU_JAR) return emptyList()
        return fetchFromUrl(hub.url)
    }

    suspend fun fetchAllHubs(): List<LegadoSource> = withContext(Dispatchers.IO) {
        val allSources = mutableListOf<LegadoSource>()
        for (hub in KNOWN_HUBS.filter { it.type == HubType.LEGADO_JSON }) {
            try {
                allSources.addAll(fetchFromHub(hub))
            } catch (e: Exception) { e.printStackTrace() }
        }
        allSources.distinctBy { it.bookSourceUrl }
    }

    fun importFromString(json: String): List<LegadoSource> {
        return LegadoSource.fromJsonArray(json.trim())
    }
}

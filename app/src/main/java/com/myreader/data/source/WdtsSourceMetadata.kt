package com.myreader.data.source

import org.json.JSONObject
import java.io.File
import java.io.InputStream

/**
 * WDTS（我的听书）书源数据模型
 *
 * 与 LegadoSource 不同，WDTS 源有自己的格式：
 * - 元数据 JSON 包含 version, entry_package, download_url
 * - 实际源逻辑封装在 JAR 插件中
 * - 本项目尝试从 JAR 中提取搜索配置，或使用元数据构建简单搜索
 */
data class WdtsSourceMetadata(
    val version: Int = 0,
    val entryPackage: String = "",
    val downloadUrl: String = "",
    val updateMsg: String = "",
    val supportUrl: String = "",
) {
    companion object {
        fun fromJson(json: String): WdtsSourceMetadata? {
            return try {
                val obj = JSONObject(json)
                WdtsSourceMetadata(
                    version = obj.optInt("version", 0),
                    entryPackage = obj.optString("entry_package", ""),
                    downloadUrl = obj.optString("download_url", ""),
                    updateMsg = obj.optString("update_msg", ""),
                    supportUrl = obj.optString("support_url", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * WDTS 源的状态
 */
enum class WdtsSourceStatus {
    /** 仅获取了元数据，JAR尚未下载 */
    METADATA_ONLY,
    /** JAR已下载，尚未解析 */
    DOWNLOADED,
    /** 已从JAR中提取出搜索配置 */
    EXTRACTED,
    /** 下载失败 */
    DOWNLOAD_FAILED,
    /** 提取失败，回退到简单HTTP模式 */
    FALLBACK_HTTP
}

/**
 * 从 JAR 中提取的搜索规则（尽可能转换为通用格式）
 */
data class WdtsExtractedRule(
    val name: String = "",
    val author: String = "",
    val siteName: String = "",
    val searchUrl: String = "",
    /** 搜索结果的CSS选择器列表 */
    val bookListCss: String = "",
    /** 书名的CSS选择器 */
    val nameCss: String = "",
    /** 作者的CSS选择器 */
    val authorCss: String = "",
    /** 封面的CSS选择器 */
    val coverCss: String = "",
    /** 详情链接的CSS选择器 */
    val detailCss: String = "",
    /** 用户代理 */
    val userAgent: String = "",
    /** 编码 */
    val charset: String = "UTF-8",
    /** 是否为原始Legado格式规则 */
    val rawRules: String? = null
)

/**
 * 完整的 WDTS 源条目
 */
data class WdtsSource(
    val metadata: WdtsSourceMetadata,
    val hubUrl: String,  // 元数据来源URL（如 shun.json）
    val hubName: String,  // 社区名称
    val status: WdtsSourceStatus = WdtsSourceStatus.METADATA_ONLY,
    val extractedRule: WdtsExtractedRule? = null,
    val jarLocalPath: String? = null,
    val errorMsg: String? = null,
    var enabled: Boolean = true
) {
    val id: String
        get() = "${hubName}_${metadata.entryPackage}"

    val displayName: String
        get() = "[${hubName}] ${metadata.entryPackage}"

    val isValidForSearch: Boolean
        get() = when (status) {
            WdtsSourceStatus.EXTRACTED -> extractedRule != null && extractedRule.searchUrl.isNotBlank()
            WdtsSourceStatus.FALLBACK_HTTP -> extractedRule != null && extractedRule.searchUrl.isNotBlank()
            else -> false
        }
}

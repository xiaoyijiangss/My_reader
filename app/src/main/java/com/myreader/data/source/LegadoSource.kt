package com.myreader.data.source

import org.json.JSONArray
import org.json.JSONObject

/**
 * Legado/阅读 兼容的书源数据模型
 *
 * 兼容「阅读」APP 的 JSON 书源格式，可导入社区维护的数万个书源。
 * 规则 DSL 参考: https://github.com/gedoor/legado
 */
data class LegadoSource(
    val bookSourceUrl: String = "",
    val bookSourceName: String = "",
    val bookSourceGroup: String = "",
    val searchUrl: String = "",
    val bookSourceType: Int = 0,
    val ruleSearch: LegadoSearchRule? = null,
    val ruleBookInfo: LegadoBookInfoRule? = null,
    val ruleToc: String = "",
    val ruleContent: LegadoContentRule? = null,
    val httpUserAgent: String = "",
    var enabled: Boolean = true
) {
    val id: String
        get() = bookSourceUrl.replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .replace("/", "_")
            .replace(".", "_")
            .let { if (it.length > 40) it.take(40) else it }

    val isValid: Boolean
        get() = bookSourceUrl.isNotBlank() && searchUrl.isNotBlank() && ruleSearch != null

    /** 从 JSON 字符串解析单个源 */
    companion object {
        fun fromJson(json: String): LegadoSource? {
            return try {
                val obj = JSONObject(json)
                parse(obj)
            } catch (e: Exception) {
                null
            }
        }

        fun fromJsonObject(obj: JSONObject): LegadoSource = parse(obj)

        fun fromJsonArray(json: String): List<LegadoSource> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { i ->
                    try {
                        parse(arr.getJSONObject(i))
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                // 尝试作为单对象解析
                fromJson(json)?.let { listOf(it) } ?: emptyList()
            }
        }

        private fun parse(obj: JSONObject): LegadoSource {
            return LegadoSource(
                bookSourceUrl = obj.optString("bookSourceUrl", ""),
                bookSourceName = obj.optString("bookSourceName", "未命名书源"),
                bookSourceGroup = obj.optString("bookSourceGroup", ""),
                searchUrl = obj.optString("searchUrl", ""),
                bookSourceType = obj.optInt("bookSourceType", 0),
                ruleSearch = obj.optJSONObject("ruleSearch")?.let { parseSearchRule(it) },
                ruleBookInfo = obj.optJSONObject("ruleBookInfo")?.let { parseBookInfoRule(it) },
                ruleToc = obj.optString("ruleToc", ""),
                ruleContent = obj.optJSONObject("ruleContent")?.let { parseContentRule(it) },
                httpUserAgent = obj.optString("httpUserAgent", ""),
                enabled = obj.optBoolean("enabled", true)
            )
        }

        private fun parseSearchRule(obj: JSONObject) = LegadoSearchRule(
            bookList = obj.optString("bookList", ""),
            name = obj.optString("name", ""),
            author = obj.optString("author", ""),
            coverUrl = obj.optString("coverUrl", ""),
            detailUrl = obj.optString("detailUrl", ""),
            kind = obj.optString("kind", "")
        )

        private fun parseBookInfoRule(obj: JSONObject) = LegadoBookInfoRule(
            name = obj.optString("name", ""),
            author = obj.optString("author", ""),
            coverUrl = obj.optString("coverUrl", ""),
            intro = obj.optString("intro", ""),
            kind = obj.optString("kind", "")
        )

        private fun parseContentRule(obj: JSONObject) = LegadoContentRule(
            content = obj.optString("content", "")
        )
    }
}

data class LegadoSearchRule(
    val bookList: String = "",
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val detailUrl: String = "",
    val kind: String = ""
)

data class LegadoBookInfoRule(
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = ""
)

data class LegadoContentRule(
    val content: String = ""
)

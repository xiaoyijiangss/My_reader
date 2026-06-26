package com.myreader.data.source

import org.json.JSONArray
import org.json.JSONObject

/**
 * 预置有声书源 — Legado JSON 格式
 * 
 * 这些源基于「我的听书」社区所覆盖的有声书网站，用 Legado 规则 DSL 实现。
 * 注意：部分网站可能有反爬机制，需要配合正确的 User-Agent 使用。
 * 如果搜索无结果，建议从 Legado 社区 (yckceo.com) 获取最新维护的书源。
 */
object PrebuiltAudiobookSources {

    fun allSources(): List<LegadoSource> {
        val jsonArray = JSONArray()
        ALL_SOURCE_DEFS.forEach { jsonArray.put(it) }
        return LegadoSource.fromJsonArray(jsonArray.toString())
    }

    fun allSourcesJson(): String {
        val jsonArray = JSONArray()
        ALL_SOURCE_DEFS.forEach { jsonArray.put(it) }
        return jsonArray.toString(2)
    }

    // ==================== 源定义 ====================
    // 规则格式说明: "CSS选择器@属性"
    //   - "h3 a@text"  → 选中 h3 a 元素，取文本
    //   - "img@src"    → 选中 img，取 src 属性
    //   - "a@href"     → 选中 a，取 href 属性
    // ================================================

    private val ALL_SOURCE_DEFS: List<JSONObject>
        get() = listOf(
            // ===== 听书网 (ting55.com) =====
            ting55,
            // ===== 听书阁 (tingshuge.com) =====
            tingshuge,
            // ===== 听89 (ting89.com) =====
            ting89,
            // ===== 听中国 (tingchina.com) =====
            tingchina,
            // ===== 520听书 (520tingshu.com) =====
            tings520,
            // ===== 幻听书 (huantingshu.com) =====
            huantingshu,
        )

    private val ting55 = JSONObject().apply {
        put("bookSourceUrl", "https://www.ting55.com")
        put("bookSourceName", "听书网")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.ting55.com/search.html?keyword={{key}}")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "div.list-works ul li")
            put("name", "a@text")
            put("author", "p.author@text")
            put("coverUrl", "img@src")
            put("detailUrl", "a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "p.announcer@text")
            put("coverUrl", "div.book-img img@src")
            put("intro", "div.intro@text")
        })
        put("ruleToc", "div.plist ul li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }

    private val tingshuge = JSONObject().apply {
        put("bookSourceUrl", "https://www.tingshuge.com")
        put("bookSourceName", "听书阁")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.tingshuge.com/search.html?keyword={{key}}")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "div.so_list ul li")
            put("name", "h3 a@text")
            put("author", "p.author@text")
            put("coverUrl", "img@src")
            put("detailUrl", "h3 a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "p.author@text")
            put("coverUrl", "div.book-img img@src")
            put("intro", "div.book-des@text")
        })
        put("ruleToc", "div.mulu ul li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }

    private val ting89 = JSONObject().apply {
        put("bookSourceUrl", "https://www.ting89.com")
        put("bookSourceName", "听89")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.ting89.com/search.asp?searchword={{key}}&searchtype=-1")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "div.news-list ul li")
            put("name", "a@text")
            put("author", "span.author@text")
            put("coverUrl", "img@src")
            put("detailUrl", "a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "p.actor@text")
            put("coverUrl", "div.pic img@src")
            put("intro", "div.jianjie@text")
        })
        put("ruleToc", "div.playlist ul li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }

    private val tingchina = JSONObject().apply {
        put("bookSourceUrl", "https://www.tingchina.com")
        put("bookSourceName", "听中国")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.tingchina.com/search.asp?keyword={{key}}&searchtype=-1")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "div.list ul li")
            put("name", "a@text")
            put("author", "span.s4@text")
            put("coverUrl", "img@src")
            put("detailUrl", "a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "p.announcer@text")
            put("coverUrl", "div.pic img@src")
            put("intro", "div.jieshao_content@text")
        })
        put("ruleToc", "div.plist ul li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }

    private val tings520 = JSONObject().apply {
        put("bookSourceUrl", "https://www.520tingshu.com")
        put("bookSourceName", "520听书")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.520tingshu.com/search?keyword={{key}}")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "div.result-list li")
            put("name", "h4 a@text")
            put("author", "span.author@text")
            put("coverUrl", "img@src")
            put("detailUrl", "h4 a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "div.author@text")
            put("coverUrl", "div.pic img@src")
            put("intro", "div.desc@text")
        })
        put("ruleToc", "div.mulu ul li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }

    private val huantingshu = JSONObject().apply {
        put("bookSourceUrl", "https://www.huantingshu.com")
        put("bookSourceName", "幻听书")
        put("bookSourceGroup", "有声小说-预置")
        put("bookSourceType", 0)
        put("searchUrl", "https://www.huantingshu.com/search/?q={{key}}")
        put("httpUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        put("ruleSearch", JSONObject().apply {
            put("bookList", "ul.s-list div.item")
            put("name", "div.title a@text")
            put("author", "span.author@text")
            put("coverUrl", "img@src")
            put("detailUrl", "div.title a@href")
        })
        put("ruleBookInfo", JSONObject().apply {
            put("name", "h1@text")
            put("author", "span.author@text")
            put("coverUrl", "img.pic@src")
            put("intro", "div.des@text")
        })
        put("ruleToc", "ul.list li a@href")
        put("ruleContent", JSONObject().apply {
            put("content", "audio source@src")
        })
    }
}

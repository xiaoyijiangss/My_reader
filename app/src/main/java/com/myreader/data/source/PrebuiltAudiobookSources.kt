package com.myreader.data.source

import org.json.JSONArray
import org.json.JSONObject

/**
 * 预置有声书源 — Legado JSON 格式
 *
 * 这些源对应「我的听书」社区 JAR 书源所覆盖的主流有声书网站，
 * 用 Legado 规则 DSL / CSS 选择器实现，无需 JAR 加载。
 *
 * 来源说明：
 * - ting55:    听书网 (资源最丰富)
 * - ting89:    听89
 * - lyshuba:   懒人书吧
 * - tingshuge: 听书阁
 * - tingchina: 听中国
 * - 23ts:      二三听书 (22听书网系列)
 * - 520tings:  520听书网
 * - huantingshu: 幻听书
 * - shengmeng: 声梦有声
 */
object PrebuiltAudiobookSources {

    /**
     * 返回所有预置的 Legado 兼容 JSON 源
     */
    fun allSources(): List<LegadoSource> {
        val jsonArray = JSONArray()
        ALL_SOURCE_DEFS.forEach { jsonArray.put(it) }
        return LegadoSource.fromJsonArray(jsonArray.toString())
    }

    /**
     * 返回 JSON 文本，供 SourceManager 存储
     */
    fun allSourcesJson(): String {
        val jsonArray = JSONArray()
        ALL_SOURCE_DEFS.forEach { jsonArray.put(it) }
        return jsonArray.toString(2)
    }

    // ==================== 源定义 ====================

    private val ALL_SOURCE_DEFS: List<JSONObject>
        get() = listOf(
            // ===== 听书网 ting55.com (资源最多) =====
            ting55_v1,    // PC 搜索页
            ting55_v2,    // 移动版搜索

            // ===== 听89 ting89.com =====
            ting89,

            // ===== 懒人书吧 lrbook.net =====
            lrbook,

            // ===== 听书阁 tingshuge.com =====
            tingshuge,

            // ===== 听中国 tingchina.com =====
            tingchina,

            // ===== 二三听书 23ts.cn =====
            ts23,

            // ===== 520听书网 520tings.com =====
            tings520,

            // ===== 幻听书 huantingshu.com =====
            huantingshu,

            // ===== 声梦有声 shengmeng.net =====
            shengmeng,

            // ===== 听音乐 tingyinyue.com =====
            tingyinyue,
        )

    // =============================================
    //  听书网 (ting55.com) - PC版
    // =============================================
    private val ting55_v1: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.ting55.com")
            put("bookSourceName", "听书网(PC)")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.ting55.com/search.html?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "ul#search-result-list li")
                put("name", "h3 a@text")
                put("author", "p.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "h3 a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "p.aut@text")
                put("coverUrl", "img.book-pic@src")
                put("intro", "div.intro@text")
            })
            put("ruleToc", "div.plist ul li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  听书网 (ting55.com) - 移动版 / 兼容版
    // =============================================
    private val ting55_v2: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://m.ting55.com")
            put("bookSourceName", "听书网(移动)")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://m.ting55.com/search?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.book-list div.item")
                put("name", "div.title@text")
                put("author", "div.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "div.author@text")
                put("coverUrl", "img.cover@src")
                put("intro", "div.intro@text")
            })
            put("ruleToc", "ul.chapter-list li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  听89 (ting89.com)
    // =============================================
    private val ting89: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.ting89.com")
            put("bookSourceName", "听89")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.ting89.com/search.asp?searchword={{key}}&searchtype=-1")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.news-list ul li")
                put("name", "a@text")
                put("author", "span.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "p.aut@text")
                put("coverUrl", "div.img img@src")
                put("intro", "div.jianjie@text")
            })
            put("ruleToc", "div.playlist ul li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  懒人书吧 (lrbook.net) — 类懒人听书站点
    // =============================================
    private val lrbook: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.lrbook.net")
            put("bookSourceName", "懒人书吧")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.lrbook.net/search?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.book-list div.item")
                put("name", "h4 a@text")
                put("author", "p.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "h4 a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "p.author@text")
                put("coverUrl", "img.pic@src")
                put("intro", "div.intro@text")
            })
            put("ruleToc", "ul.playlist li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  听书阁 (tingshuge.com)
    // =============================================
    private val tingshuge: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.tingshuge.com")
            put("bookSourceName", "听书阁")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.tingshuge.com/search.html?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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

    // =============================================
    //  听中国 (tingchina.com) — 资源丰富，更新快
    // =============================================
    private val tingchina: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.tingchina.com")
            put("bookSourceName", "听中国")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.tingchina.com/search.asp?keyword={{key}}&searchtype=-1")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.list ul li")
                put("name", "a@text")
                put("author", "span.s4@text")
                put("coverUrl", "img@src")
                put("detailUrl", "a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "p.aut@text")
                put("coverUrl", "div.img img@src")
                put("intro", "div.jieshao_content@text")
            })
            put("ruleToc", "div.plist ul li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  二三听书 (23ts.cn / 22听书系列)
    // =============================================
    private val ts23: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.23ts.cn")
            put("bookSourceName", "二三听书")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.23ts.cn/search?q={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.search-results div.item")
                put("name", "h3 a@text")
                put("author", "span.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "h3 a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "span.author@text")
                put("coverUrl", "img.cover@src")
                put("intro", "div.desc@text")
            })
            put("ruleToc", "ul.episodes li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  520听书网 (520tings.com)
    // =============================================
    private val tings520: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.520tings.com")
            put("bookSourceName", "520听书")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.520tings.com/search?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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

    // =============================================
    //  幻听书 (huantingshu.com)
    // =============================================
    private val huantingshu: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.huantingshu.com")
            put("bookSourceName", "幻听书")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.huantingshu.com/search/?q={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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

    // =============================================
    //  声梦有声 (shengmeng.net)
    // =============================================
    private val shengmeng: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.shengmeng.net")
            put("bookSourceName", "声梦有声")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.shengmeng.net/search?key={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "ul.search-list li")
                put("name", "h3 a@text")
                put("author", "p.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "h3 a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "div.info p@text")
                put("coverUrl", "img@src")
                put("intro", "div.intro@text")
            })
            put("ruleToc", "ul.playlist li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }

    // =============================================
    //  听音乐 (tingyinyue.com) — 有声小说
    // =============================================
    private val tingyinyue: JSONObject
        get() = JSONObject().apply {
            put("bookSourceUrl", "https://www.tingyinyue.com")
            put("bookSourceName", "听音乐")
            put("bookSourceGroup", "有声小说-内置")
            put("bookSourceType", 0)
            put("searchUrl", "https://www.tingyinyue.com/search?keyword={{key}}")
            put("httpUserAgent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            put("ruleSearch", JSONObject().apply {
                put("bookList", "div.result div.item")
                put("name", "h4 a@text")
                put("author", "span.author@text")
                put("coverUrl", "img@src")
                put("detailUrl", "h4 a@href")
            })
            put("ruleBookInfo", JSONObject().apply {
                put("name", "h1@text")
                put("author", "span.authors@text")
                put("coverUrl", "img.book-pic@src")
                put("intro", "div.des@text")
            })
            put("ruleToc", "div.chapter ul li a@href")
            put("ruleContent", JSONObject().apply {
                put("content", "audio source@src")
            })
        }
}

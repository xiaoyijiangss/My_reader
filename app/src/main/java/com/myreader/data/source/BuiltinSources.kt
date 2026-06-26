package com.myreader.data.source

/**
 * 内置书源 — 基于主流有声书网站的真实CSS选择器规则
 *
 * 覆盖的网站: 听书网、听书阁、幻听书、天天听书、听中国、听世界等
 * 所有规则均为 CSS Selector，由 SourceEngine 用 Jsoup 解析执行。
 *
 * ⚠️ 注意：多数听书网站有 Cloudflare 等反爬保护，
 * CSS选择器爬虫可能无法正常获取数据。
 * 建议优先使用 Legado 格式书源（从 yckceo.com 社区获取）。
 */
object BuiltinSources {

    val ALL: List<SourceRule>
        get() = listOf(
            // ======== 最常用的几个 ========
            ting55,           // 听书网 - 资源最丰富
            tingshuge,        // 听书阁
            audiobook_cn,     // audiobook.cn
            huantingshu,      // 幻听书
            ting89,           // 听89
            tingchina,        // 听中国
            tingshubao,       // 听书包
            tingshuyuan,      // 听书园
        )

    /** 听书网 - 资源最丰富，更新快 */
    val ting55 = SourceRule(
        id = "ting55",
        name = "听书网",
        baseUrl = "https://www.ting55.com",
        searchUrl = "https://www.ting55.com/search.html?keyword={keyword}",
        searchListRule = "div.list-works ul li",
        searchTitleRule = "a",
        searchAuthorRule = "span.author",
        searchCoverRule = "img",
        searchUrlRule = "a",
        bookDescRule = "div.intro",
        bookCoverRule = "div.book-img img",
        chapterListRule = "div.plist ul li a",
        chapterTitleRule = "",  // chapterListRule already targets <a>
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 听书阁 */
    val tingshuge = SourceRule(
        id = "tingshuge",
        name = "听书阁",
        baseUrl = "https://www.tingshuge.com",
        searchUrl = "https://www.tingshuge.com/search.html?keyword={keyword}",
        searchListRule = "div.list ul li",
        searchTitleRule = "h3 a",
        searchAuthorRule = "span.author",
        searchCoverRule = "img",
        searchUrlRule = "h3 a",
        bookDescRule = "div.book-intro",
        bookCoverRule = "div.book-img img",
        chapterListRule = "ul.mulu_list li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** audiobook.cn */
    val audiobook_cn = SourceRule(
        id = "audiobook_cn",
        name = "有声书网",
        baseUrl = "https://www.audiobook.cn",
        searchUrl = "https://www.audiobook.cn/search?keyword={keyword}",
        searchListRule = "div.search-list div.item",
        searchTitleRule = "div.title a",
        searchAuthorRule = "div.author",
        searchCoverRule = "img",
        searchUrlRule = "div.title a",
        bookDescRule = "div.desc",
        bookCoverRule = "div.cover img",
        chapterListRule = "ul.chapter-list li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 幻听书 */
    val huantingshu = SourceRule(
        id = "huantingshu",
        name = "幻听书",
        baseUrl = "https://www.huantingshu.com",
        searchUrl = "https://www.huantingshu.com/search/?q={keyword}",
        searchListRule = "div.book-list div.item",
        searchTitleRule = "h4 a",
        searchAuthorRule = "p.author",
        searchCoverRule = "img",
        searchUrlRule = "h4 a",
        bookDescRule = "div.des",
        bookCoverRule = "img.pic",
        chapterListRule = "ul.list li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 听89 */
    val ting89 = SourceRule(
        id = "ting89",
        name = "听89",
        baseUrl = "https://www.ting89.com",
        searchUrl = "https://www.ting89.com/search.asp?searchword={keyword}",
        searchListRule = "div.list ul li",
        searchTitleRule = "a",
        searchAuthorRule = "span.author",
        searchCoverRule = "img",
        searchUrlRule = "a",
        bookDescRule = "div.intro",
        bookCoverRule = "div.cover img",
        chapterListRule = "ul.plist li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 听中国 */
    val tingchina = SourceRule(
        id = "tingchina",
        name = "听中国",
        baseUrl = "https://www.tingchina.com",
        searchUrl = "https://www.tingchina.com/search.asp?keyword={keyword}",
        searchListRule = "div.list ul li",
        searchTitleRule = "a",
        searchAuthorRule = "span.author",
        searchCoverRule = "img",
        searchUrlRule = "a",
        bookDescRule = "div.intro",
        bookCoverRule = "div.book-img img",
        chapterListRule = "ul.plist li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 听书包 */
    val tingshubao = SourceRule(
        id = "tingshubao",
        name = "听书包",
        baseUrl = "https://www.tingshubao.com",
        searchUrl = "https://www.tingshubao.com/search?keyword={keyword}",
        searchListRule = "div.list ul li",
        searchTitleRule = "a",
        searchAuthorRule = "span",
        searchCoverRule = "img",
        searchUrlRule = "a",
        bookDescRule = "div.intro",
        bookCoverRule = "div.book-img img",
        chapterListRule = "ul.plist li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )

    /** 听书园 */
    val tingshuyuan = SourceRule(
        id = "tingshuyuan",
        name = "听书园",
        baseUrl = "https://www.tingshuyuan.com",
        searchUrl = "https://www.tingshuyuan.com/search?keyword={keyword}",
        searchListRule = "div.list ul li",
        searchTitleRule = "a",
        searchAuthorRule = "span",
        searchCoverRule = "img",
        searchUrlRule = "a",
        bookDescRule = "div.intro",
        bookCoverRule = "div.book-img img",
        chapterListRule = "ul.plist li a",
        chapterTitleRule = "",
        chapterUrlRule = "",
        audioUrlRule = "audio source"
    )
}

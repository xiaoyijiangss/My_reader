package com.myreader.data.source

/**
 * 书源规则定义——类似"阅读"APP的书源机制
 *
 * 每条规则定义了一个有声书网站如何被解析：
 * - 如何搜索
 * - 如何获取书籍详情
 * - 如何获取章节列表
 * - 如何获取音频URL
 */
data class SourceRule(
    val id: String,
    val name: String,
    val baseUrl: String,
    val searchUrl: String,           // 搜索URL，用 {keyword} 占位
    val charset: String = "UTF-8",
    val enabled: Boolean = true,

    // ---- 搜索列表页规则 ----
    val searchListRule: String,       // 搜索结果列表CSS选择器
    val searchTitleRule: String,      // 标题CSS选择器
    val searchAuthorRule: String,     // 作者CSS选择器
    val searchCoverRule: String,      // 封面CSS选择器（取src/abs:src等）
    val searchUrlRule: String,        // 详情页URL CSS选择器（取href）

    // ---- 详情页规则 ----
    val bookDescRule: String = "",    // 简介CSS选择器
    val bookCoverRule: String = "",   // 详情页封面CSS选择器

    // ---- 章节列表规则 ----
    val chapterListRule: String,      // 章节列表容器CSS选择器
    val chapterTitleRule: String,     // 章节标题CSS选择器
    val chapterUrlRule: String,       // 章节URL CSS选择器

    // ---- 音频规则 ----
    val audioUrlRule: String = "",     // 音频URL正则或CSS选择器
    val audioUrlAttr: String = "src"  // 音频URL属性名（src、data-src等）
)

package com.myreader.data.source

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Legado 规则解析器
 *
 * 将「阅读」APP 的规则 DSL 转换为 Jsoup 可执行的 CSS 选择器。
 * 支持的规则语法:
 *   - css;<selector>         CSS选择器（默认）
 *   - class.<name>           类选择器 → .name
 *   - id.<name>              ID选择器 → #name
 *   - tag.<name>             标签选择器
 *   - @<attr>                属性提取（@text=文本, @href, @src, @data-src等）
 *   - 组合: class.list@li     → .list li
 *   - 多个: rule1&&rule2     取合并结果
 *   - 或:   rule1||rule2     取第一个非空
 */
object LegadoRuleParser {

    /**
     * 解析规则字符串为 CSS 选择器
     * @param rule Legado规则字符串
     * @return CSS选择器字符串
     */
    fun toCssSelector(rule: String): String {
        if (rule.isBlank()) return ""

        // 如果包含 || 或 &&，拆分处理
        // 简化: 取第一部分
        val parts = rule.split("&&", "||").map { it.trim() }
            .filter { it.isNotBlank() }

        // 简单情况：没有 JS 表达式的纯 CSS 规则
        return if (rule.startsWith("css;")) {
            rule.removePrefix("css;").trim()
        } else if (rule.startsWith("@")) {
            "" // 纯JS规则，跳过
        } else if (rule.contains("{") || rule.contains("}")) {
            // 包含JS表达式，尝试提取CSS部分
            extractCssFromJsRule(rule)
        } else {
            convertLegadoSyntax(rule)
        }
    }

    /**
     * 从规则中提取属性名
     * @param rule Legado规则字符串
     * @return 属性名（如 "src", "href", "text"）
     */
    fun extractAttr(rule: String): String {
        // 查找最后一个 @attr 部分
        val lastAt = rule.lastIndexOf('@')
        if (lastAt < 0) return "text"

        val afterAt = rule.substring(lastAt + 1)
            .trim()

        return when {
            afterAt.startsWith("text") -> "text"
            afterAt.startsWith("href") -> "href"
            afterAt.startsWith("src") -> "src"
            afterAt.startsWith("data-") -> afterAt.takeWhile { it.isLetterOrDigit() || it == '-' }
            afterAt.startsWith("content") -> "content"
            afterAt.startsWith("abs:href") -> "abs:href"
            afterAt.startsWith("abs:src") -> "abs:src"
            afterAt.startsWith("{") -> "" // JS表达式
            else -> afterAt.takeWhile { it.isLetterOrDigit() }
        }
    }

    /**
     * 将 Legado 语法转为标准 CSS 选择器
     * 如: class.bookList@li → .bookList li
     *     id.content@tag.p → #content p
     */
    private fun convertLegadoSyntax(rule: String): String {
        // 先去掉 @attr 部分
        val withoutAttr = rule.substringBeforeLast("@")
            .replace(Regex("@\\{.*?}"), "") // 去掉 @{js} 
            .replace(Regex("@\\w+"), "")     // 去掉 @attr
            .trim()

        if (withoutAttr.isBlank()) return ""

        // 处理 class.xx → .xx
        // 处理 id.xx → #xx
        // 处理 tag.xx → xx
        val parts = withoutAttr.split(Regex("\\s+"))
        return parts.joinToString(" ") { part ->
            when {
                part.startsWith("class.") -> "." + part.removePrefix("class.")
                part.startsWith("id.") -> "#" + part.removePrefix("id.")
                part.startsWith("tag.") -> part.removePrefix("tag.")
                part.startsWith(".") || part.startsWith("#") -> part
                else -> part
            }
        }
    }

    /**
     * 从包含JS的规则中提取CSS部分
     */
    private fun extractCssFromJsRule(rule: String): String {
        // 提取尖括号中的CSS: <js>result</js> 或类似
        // 简化处理
        return rule.replace(Regex("\\{.*?}"), "").trim()
    }

    // ---- 便捷方法 ----

    /** 获取元素列表中某项的文本 */
    fun getText(el: Element?, rule: String): String {
        if (el == null) return ""
        val css = toCssSelector(rule)
        return if (css.isBlank()) {
            val attr = extractAttr(rule)
            if (attr == "text") el.text().trim()
            else el.attr(attr)
        } else {
            val sub = el.select(css).first()
            if (sub != null) {
                val attr = extractAttr(rule)
                if (attr == "text") sub.text().trim()
                else sub.attr(attr)
            } else ""
        }
    }

    /** 获取元素列表中某项的属性 */
    fun getAttr(el: Element?, rule: String): String {
        if (el == null) return ""
        val css = toCssSelector(rule)
        val attr = extractAttr(rule)

        return if (css.isBlank()) {
            if (attr == "text") el.text().trim()
            else el.attr(attr)
        } else {
            val sub = el.select(css).first()
            sub?.let {
                if (attr == "text") it.text().trim()
                else it.attr(attr)
            } ?: ""
        }
    }

    /** 获取元素列表 */
    fun selectList(doc: Elements, rule: String): Elements {
        val css = toCssSelector(rule)
        return if (css.isBlank()) doc
        else doc.select(css)
    }

    /** 获取元素列表中某项的URL属性 */
    fun getUrl(el: Element?, rule: String): String {
        if (el == null) return ""
        val href = getAttr(el, rule)
        // 如果attr已经是abs:href直接返回
        return if (extractAttr(rule) == "abs:href") href
        else el.getElementsByAttribute("href").first()?.attr("abs:href")
            ?: if (href.startsWith("http")) href
            else el.attr("abs:href").ifBlank { href }
    }
}

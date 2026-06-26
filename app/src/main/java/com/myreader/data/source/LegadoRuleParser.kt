package com.myreader.data.source

import android.util.Log
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Legado 规则解析器
 *
 * 将「阅读」APP 的规则 DSL 转换为 Jsoup 可执行的 CSS 选择器。
 *
 * 支持的规则语法:
 *   - css;<selector>              CSS选择器（默认）
 *   - class.<name>@tag.<name>     类+标签组合
 *   - id.<name>                   ID选择器 → #name
 *   - tag.<name>                  标签选择器
 *   - .<css-selector>            原生CSS选择器(以.或#开头)
 *   - @<attr>                     属性提取（@text=文本, @href, @src, etc.）
 *   - ||                          或（取第一个非空）
 *   - &&                          并（合并结果）
 *   - js:<expression>             JS表达式（暂不支持，跳过）
 *   - @js:<expression>            JS属性提取（暂不支持，取text）
 *   - <js>expression</js>         JS内嵌（暂不支持，跳过）
 *
 * 注意：这是一个简化实现，不支持完整的 Legado 规则 DSL。
 *       对于无法解析的复杂规则，会返回空字符串并回退到 body 子元素。
 */
object LegadoRuleParser {

    private const val TAG = "LegadoRuleParser"

    /**
     * 解析规则字符串为 CSS 选择器
     * @param rule Legado规则字符串
     * @return CSS选择器字符串
     */
    fun toCssSelector(rule: String): String {
        if (rule.isBlank()) return ""

        val trimmed = rule.trim()

        // 1. css; 前缀 → 直接取CSS
        if (trimmed.startsWith("css;")) {
            return trimmed.removePrefix("css;").trim()
        }

        // 2. js: 前缀 → JS表达式，无法转CSS
        if (trimmed.startsWith("js:") || trimmed.startsWith("<js>") || trimmed.startsWith("@js:")) {
            Log.d(TAG, "JS规则无法转CSS: ${trimmed.take(80)}")
            return ""
        }

        // 3. 处理 || 和 && 组合规则
        //    || → 取第一个部分（OR逻辑）
        //    && → 取第一个部分（AND逻辑），后续需要merge
        if (trimmed.contains("||")) {
            val firstPart = trimmed.split("||").first().trim()
            Log.d(TAG, "||规则取第一部分: $firstPart")
            return toCssSelector(firstPart) // 递归解析
        }
        if (trimmed.contains("&&")) {
            // && 连接多个选择器，需要全部解析。简化处理：取CSS连接
            val parts = trimmed.split("&&").map { it.trim() }.filter { it.isNotBlank() }
            val cssParts = parts.mapNotNull { p ->
                val css = tryToCssSelector(p)
                if (css.isBlank()) null else css
            }
            if (cssParts.isNotEmpty()) {
                return cssParts.joinToString(", ")
            }
            // 如果都转不了，尝试第一部分
            return tryToCssSelector(parts.first())
        }

        // 4. 单条规则解析
        return tryToCssSelector(trimmed)
    }

    /**
     * 尝试将单条规则转为CSS（不处理 || 和 &&）
     */
    private fun tryToCssSelector(rule: String): String {
        // 去掉 JS 表达式（花括号内容）
        val cleanedRule = rule
            .replace(Regex("@\\{[^}]*\\}"), "")   // 去掉 @{js}
            .replace(Regex("\\{\\{[^}]*\\}\\}"), "") // 去掉 {{variable}}
            .replace(Regex("<js>[^<]*</js>"), "")   // 去掉 <js>...</js>
            .replace(Regex("@js:[^\\s@]*"), "")     // 去掉 @js:xxx
            .trim()

        if (cleanedRule.isBlank()) return ""

        // 去掉最后的 @attr 部分（但不影响中间的选择器）
        val lastAtIndex = findLastAttrIndex(cleanedRule)
        val selectorPart = if (lastAtIndex >= 0) {
            cleanedRule.substring(0, lastAtIndex).trim()
        } else {
            cleanedRule
        }

        if (selectorPart.isBlank()) return ""

        return convertLegadoSyntax(selectorPart)
    }

    /**
     * 找到最后一个 @attr 的位置（@后跟字母或text等）
     */
    private fun findLastAttrIndex(rule: String): Int {
        var i = rule.length - 1
        while (i >= 0) {
            if (rule[i] == '@') {
                // 检查前面不是转义符
                if (i == 0 || rule[i - 1] != '\\') {
                    // 检查@后面紧跟的是合法的属性名或text
                    val after = rule.substring(i + 1)
                    if (after.isNotEmpty() && (
                        after.startsWith("text") || after.startsWith("href") ||
                        after.startsWith("src") || after.startsWith("abs:") ||
                        after.startsWith("content") || after.startsWith("data-") ||
                        after.startsWith("class") || after.startsWith("id") ||
                        after.first().isLetterOrDigit()
                    )) {
                        return i
                    }
                }
            }
            i--
        }
        return -1
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

        val afterAt = rule.substring(lastAt + 1).trim()

        return when {
            afterAt.startsWith("text") -> "text"
            afterAt.startsWith("href") -> "href"
            afterAt.startsWith("src") -> "src"
            afterAt.startsWith("data-") -> afterAt.takeWhile { it.isLetterOrDigit() || it == '-' }
            afterAt.startsWith("content") -> "content"
            afterAt.startsWith("abs:href") -> "abs:href"
            afterAt.startsWith("abs:src") -> "abs:src"
            afterAt.startsWith("tag.") || afterAt.startsWith("class.") -> {
                // @tag.xxx 或 @class.xxx 是复合规则，取分隔后的部分
                "text"
            }
            afterAt.startsWith("{") -> "" // JS表达式
            afterAt.startsWith("js:") -> "text" // @js:表达式，默认取文本
            else -> afterAt.takeWhile { it.isLetterOrDigit() || it == '-' }
        }
    }

    /**
     * 将 Legado 语法转为标准 CSS 选择器
     *
     * Legado特有语法:
     *   class.name@tag.div    → .name div
     *   id.content@tag.p      → #content p
     *   tag.li                → li
     *   .my-class             → .my-class  (保持原样)
     *   #my-id                → #my-id     (保持原样)
     *   div.my-class          → div.my-class (保持原样)
     *   普通CSS选择器          → 保持原样
     */
    private fun convertLegadoSyntax(rule: String): String {
        val tokens = tokenizeRule(rule)
        if (tokens.isEmpty()) return ""

        val cssParts = mutableListOf<String>()

        for (token in tokens) {
            when {
                // class.xxx → .xxx
                token.startsWith("class.") -> {
                    val name = token.removePrefix("class.")
                    if (name.isNotBlank()) cssParts.add(".$name")
                }
                // id.xxx → #xxx
                token.startsWith("id.") -> {
                    val name = token.removePrefix("id.")
                    if (name.isNotBlank()) cssParts.add("#$name")
                }
                // tag.xxx → xxx
                token.startsWith("tag.") -> {
                    val name = token.removePrefix("tag.")
                    if (name.isNotBlank()) cssParts.add(name)
                }
                // 已经是标准CSS选择器（以. #开头，或包含. #等）
                token.startsWith(".") || token.startsWith("#") -> cssParts.add(token)
                // 标准元素选择器（字母开头 + 可能包含.或#）
                token.first().isLetter() -> cssParts.add(token)
                // 其他（如 :nth-child, [attr]等伪类/属性选择器）
                token.startsWith(":") || token.startsWith("[") -> cssParts.add(token)
                // 无法识别的，保持原样
                else -> cssParts.add(token)
            }
        }

        return cssParts.joinToString(" ").trim()
    }

    /**
     * 将规则字符串分解为 token 列表
     * 空格分隔 + 处理 Legado 特有的 class./id./tag. 前缀
     */
    private fun tokenizeRule(rule: String): List<String> {
        val tokens = mutableListOf<String>()
        val parts = rule.split(Regex("\\s+")).filter { it.isNotBlank() }

        for (part in parts) {
            // 处理 class.xxx@tag.yyy 这种连在一起的复合语法
            if ((part.startsWith("class.") || part.startsWith("id.") || part.startsWith("tag.")) &&
                part.contains('@') && part.indexOf('@') > 0
            ) {
                // 拆分为 before@ 和 after@
                val atIdx = part.indexOf('@')
                val before = part.substring(0, atIdx)
                val after = part.substring(atIdx)
                // before 是 Legado选择器，after 将被忽略（属性部分）
                tokens.add(before)
            } else {
                tokens.add(part)
            }
        }

        return tokens
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
        val attrName = extractAttr(rule)

        // 如果是取属性模式（@href, @src, @abs:href 等）
        if (attrName != "text" || rule.contains("@")) {
            val css = toCssSelector(rule)
            val target = if (css.isNotBlank()) el.select(css).first() else el
            if (target != null) {
                if (attrName == "abs:href" || attrName == "abs:src") {
                    return target.attr(attrName)
                }
                return target.attr(attrName).ifBlank {
                    // 回退: 如果是href相关，尝试abs:href
                    if (attrName == "href") target.attr("abs:href")
                    else ""
                }
            }
        }

        // 默认尝试从元素中提取链接
        // 先取第一个 a 标签的 abs:href
        val firstLink = el.select("a[href]").first()
        if (firstLink != null) {
            return firstLink.attr("abs:href").ifBlank { firstLink.attr("href") }
        }

        // 如果元素本身就是a标签
        val selfHref = el.attr("abs:href").ifBlank { el.attr("href") }
        if (selfHref.isNotBlank()) return selfHref

        return ""
    }
}

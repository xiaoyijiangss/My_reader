package com.myreader.data.source

import android.util.Log
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class SourceEngine {

    companion object {
        private const val TAG = "SourceEngine"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 执行搜索 */
    suspend fun search(
        keyword: String,
        rules: List<SourceRule> = BuiltinSources.ALL
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val enabledRules = rules.filter { it.enabled }
        Log.i(TAG, "========== CSS搜索: \"$keyword\", 书源数=${enabledRules.size} ==========")

        val allResults = mutableListOf<SearchResult>()
        for (rule in enabledRules) {
            try {
                val results = searchSingle(keyword, rule)
                if (results.isNotEmpty()) {
                    Log.i(TAG, "  ✅ [${rule.name}] 找到 ${results.size} 个结果")
                    allResults.addAll(results)
                } else {
                    Log.w(TAG, "  ❌ [${rule.name}] 无结果")
                }
            } catch (e: Exception) {
                Log.e(TAG, "  💥 [${rule.name}] 异常: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        Log.i(TAG, "========== CSS搜索完成: 总计 ${allResults.size} 个结果 ==========")
        allResults
    }

    private fun searchSingle(keyword: String, rule: SourceRule): List<SearchResult> {
        val encoded = URLEncoder.encode(keyword, rule.charset)
        val url = rule.searchUrl.replace("{keyword}", encoded)
        Log.d(TAG, "    [${rule.name}] URL: $url")

        val doc = fetchDoc(url, rule.charset)
        val items = doc.select(rule.searchListRule)
        Log.d(TAG, "    [${rule.name}] CSS=\"${rule.searchListRule}\" 匹配: ${items.size} 个元素")

        val results = items.mapNotNull { item ->
            try {
                val title = item.select(rule.searchTitleRule).text().trim()
                val author = item.select(rule.searchAuthorRule).text().trim()
                val coverEl = item.select(rule.searchCoverRule).first()
                val coverUrl = coverEl?.let {
                    it.attr("abs:src").ifBlank { it.attr("data-src").ifBlank { it.attr("src") } }
                } ?: ""
                val detailUrl = item.select(rule.searchUrlRule).first()
                    ?.attr("abs:href") ?: ""

                if (title.isNotBlank() && detailUrl.isNotBlank()) {
                    SearchResult(
                        title = title,
                        author = author.ifBlank { "未知" },
                        coverUrl = fixUrl(coverUrl, rule.baseUrl),
                        url = fixUrl(detailUrl, rule.baseUrl),
                        sourceId = rule.id
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }

        if (results.isEmpty() && items.isNotEmpty()) {
            Log.w(TAG, "    [${rule.name}] 匹配 ${items.size} 元素但提取失败")
            Log.d(TAG, "    [${rule.name}] 首元素HTML: ${items.first()?.outerHtml()?.take(300)}")
        }
        return results
    }

    /** 获取书籍详情 */
    suspend fun getBookDetail(result: SearchResult): Book? = withContext(Dispatchers.IO) {
        val rule = BuiltinSources.ALL.find { it.id == result.sourceId } ?: return@withContext null
        try {
            val doc = fetchDoc(result.url, rule.charset)
            val description = if (rule.bookDescRule.isNotBlank()) {
                doc.select(rule.bookDescRule).text().trim()
            } else ""
            val coverUrl = if (rule.bookCoverRule.isNotBlank()) {
                val el = doc.select(rule.bookCoverRule).first()
                el?.let {
                    it.attr("abs:src").ifBlank { it.attr("src") }
                } ?: result.coverUrl
            } else result.coverUrl

            Book(
                title = result.title,
                author = result.author,
                coverUrl = fixUrl(coverUrl, rule.baseUrl),
                description = description,
                sourceId = result.sourceId,
                sourceUrl = result.url
            )
        } catch (e: Exception) {
            null
        }
    }

    /** 获取章节列表 */
    suspend fun getChapters(book: Book): List<Chapter> = withContext(Dispatchers.IO) {
        val rule = BuiltinSources.ALL.find { it.id == book.sourceId } ?: return@withContext emptyList()
        try {
            val doc = fetchDoc(book.sourceUrl, rule.charset)
            doc.select(rule.chapterListRule).mapIndexed { index, item ->
                // 如果章节Title/Url规则为空，说明chapterListRule已定位到<a>标签本身
                val title = if (rule.chapterTitleRule.isNotBlank())
                    item.select(rule.chapterTitleRule).text().trim()
                else item.text().trim()

                val href = if (rule.chapterUrlRule.isNotBlank())
                    item.select(rule.chapterUrlRule).first()?.attr("abs:href") ?: ""
                else item.attr("abs:href")

                // 如果item就是<a>标签且没有abs:href，尝试直接取href
                val url = fixUrl(
                    href.ifBlank { item.attr("href") },
                    rule.baseUrl
                )

                Chapter(
                    bookId = book.id,
                    title = title,
                    url = url,
                    index = index
                )
            }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 获取章节音频URL */
    suspend fun getAudioUrl(chapter: Chapter, sourceId: String): String? =
        withContext(Dispatchers.IO) {
            val rule = BuiltinSources.ALL.find { it.id == sourceId } ?: return@withContext null
            try {
                val doc = fetchDoc(chapter.url, rule.charset)
                if (rule.audioUrlRule.isNotBlank()) {
                    val el = doc.select(rule.audioUrlRule).first()
                    (el?.attr("abs:src") ?: "").ifBlank {
                        el.attr(rule.audioUrlAttr)
                    }?.let { fixUrl(it, rule.baseUrl) }
                } else {
                    // 自动查找 <audio> 或 <source> 标签
                    doc.select("audio source").first()?.attr("src")
                        ?: doc.select("audio").first()?.attr("src")
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun fetchDoc(url: String, charset: String): Document {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.bytes() ?: throw Exception("Empty response")
        return Jsoup.parse(String(body, Charset.forName(charset)))
    }

    private fun fixUrl(url: String, baseUrl: String): String {
        if (url.isBlank()) return ""
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> baseUrl.trimEnd('/') + url
            else -> "$baseUrl/$url"
        }
    }
}

package com.myreader.data.repository

import com.myreader.MyReaderApp
import com.myreader.data.db.entity.BookEntity
import com.myreader.data.db.entity.ChapterEntity
import com.myreader.data.source.BuiltinSources
import com.myreader.data.source.LegadoEngine
import com.myreader.data.source.SourceEngine
import com.myreader.data.source.SourceManager
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.SearchResult
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val db: com.myreader.data.db.AppDatabase = MyReaderApp.instance.database,
    private val cssEngine: SourceEngine = SourceEngine(),
    private val legadoEngine: LegadoEngine = LegadoEngine()
) {
    private val bookDao = db.bookDao()
    private val chapterDao = db.chapterDao()

    // ---- 本地书架 ----
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    // ---- 搜索（合并 CSS源 + Legado源） ----
    suspend fun searchOnline(keyword: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // 1. 搜索 CSS 内置源
        try {
            results.addAll(cssEngine.search(keyword, SourceManager.getEnabledCss()))
        } catch (e: Exception) { e.printStackTrace() }

        // 2. 搜索 Legado JSON 源
        try {
            val legadoSources = SourceManager.getEnabled()
            if (legadoSources.isNotEmpty()) {
                results.addAll(legadoEngine.search(keyword, legadoSources))
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 去重（同URL只保留一个）
        return results.distinctBy { it.url }
    }

    // ---- 书籍详情 ----
    suspend fun getBookDetail(result: SearchResult): Book? {
        // 先尝试 Legado 源
        val legadoSources = SourceManager.sources.value
        if (legadoSources.any { it.id == result.sourceId }) {
            return legadoEngine.getBookDetail(result, legadoSources)
        }
        // 回退到 CSS 源
        return cssEngine.getBookDetail(result)
    }

    // ---- 章节列表 ----
    suspend fun getChapters(book: Book): List<Chapter> {
        val legadoSources = SourceManager.sources.value
        if (legadoSources.any { it.id == book.sourceId }) {
            return legadoEngine.getChapters(book, legadoSources)
        }
        return cssEngine.getChapters(book)
    }

    // ---- 获取音频URL ----
    suspend fun getAudioUrl(chapter: Chapter, sourceId: String): String? {
        val legadoSources = SourceManager.sources.value
        if (legadoSources.any { it.id == sourceId }) {
            return legadoEngine.getAudioUrl(chapter, sourceId, legadoSources)
        }
        return cssEngine.getAudioUrl(chapter, sourceId)
    }

    // ---- 收藏/保存到书架 ----
    suspend fun addToLibrary(book: Book): Long {
        val entity = BookEntity(
            title = book.title,
            author = book.author,
            coverUrl = book.coverUrl,
            description = book.description,
            sourceId = book.sourceId,
            sourceUrl = book.sourceUrl,
            category = book.category
        )
        return bookDao.insert(entity)
    }

    suspend fun removeFromLibrary(bookId: Long) = bookDao.deleteById(bookId)

    // ---- 章节缓存 ----
    suspend fun cacheChapters(bookId: Long, chapters: List<Chapter>) {
        chapterDao.deleteByBookId(bookId)
        chapterDao.insertAll(chapters.map { it.toEntity(bookId) })
    }

    fun getCachedChapters(bookId: Long): Flow<List<ChapterEntity>> =
        chapterDao.getChaptersByBookId(bookId)

    suspend fun getCachedChaptersList(bookId: Long): List<ChapterEntity> =
        chapterDao.getChaptersListByBookId(bookId)

    // ---- 播放进度 ----
    suspend fun updateProgress(bookId: Long, chapterIndex: Int, position: Long) =
        bookDao.updateProgress(bookId, chapterIndex, position)

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getById(id)

    // ---- 扩展函数 ----
    private fun Chapter.toEntity(bookId: Long) = ChapterEntity(
        bookId = bookId,
        title = title,
        url = url,
        index = index,
        duration = duration,
        isDownloaded = isDownloaded,
        localPath = localPath
    )
}

package com.myreader.data.repository

import com.myreader.MyReaderApp
import com.myreader.data.db.entity.BookEntity
import com.myreader.data.db.entity.ChapterEntity
import com.myreader.data.source.SourceEngine
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.SearchResult
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val db: com.myreader.data.db.AppDatabase = MyReaderApp.instance.database,
    private val engine: SourceEngine = SourceEngine()
) {
    private val bookDao = db.bookDao()
    private val chapterDao = db.chapterDao()

    // ---- 本地书架 ----
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    // ---- 搜索 ----
    suspend fun searchOnline(keyword: String): List<SearchResult> = engine.search(keyword)

    // ---- 书籍详情 ----
    suspend fun getBookDetail(result: SearchResult): Book? = engine.getBookDetail(result)

    // ---- 章节列表 ----
    suspend fun getChapters(book: Book): List<Chapter> = engine.getChapters(book)

    // ---- 获取音频URL ----
    suspend fun getAudioUrl(chapter: Chapter, sourceId: String): String? =
        engine.getAudioUrl(chapter, sourceId)

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

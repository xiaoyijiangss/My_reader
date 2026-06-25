package com.myreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.data.db.entity.ChapterEntity
import com.myreader.data.repository.BookRepository
import com.myreader.model.Book
import com.myreader.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Book? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isAddedToLibrary: Boolean = false,
    val bookId: Long = 0L,
    val error: String? = null
)

class BookDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository()
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    fun loadFromSearch(searchResult: SearchResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val book = repository.getBookDetail(searchResult)
                if (book != null) {
                    _uiState.value = _uiState.value.copy(book = book, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取书籍详情失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage
                )
            }
        }
    }

    fun loadFromBookId(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isAddedToLibrary = true, bookId = bookId)
            try {
                val bookEntity = repository.getBookById(bookId)
                if (bookEntity != null) {
                    _uiState.value = _uiState.value.copy(
                        book = Book(
                            id = bookEntity.id,
                            title = bookEntity.title,
                            author = bookEntity.author,
                            coverUrl = bookEntity.coverUrl,
                            description = bookEntity.description,
                            sourceId = bookEntity.sourceId,
                            sourceUrl = bookEntity.sourceUrl
                        ),
                        isLoading = false
                    )
                    loadChapters(bookEntity.sourceId, bookEntity.sourceUrl, bookEntity.id)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "书籍不存在")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun addToLibrary() {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val bookId = repository.addToLibrary(book)
                _uiState.value = _uiState.value.copy(isAddedToLibrary = true, bookId = bookId)
                loadChapters(book.sourceId, book.sourceUrl, bookId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "添加失败: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun loadChapters(sourceId: String, sourceUrl: String, bookId: Long) {
        try {
            // 先从缓存读
            var chapters = repository.getCachedChaptersList(bookId)
            if (chapters.isEmpty()) {
                // 在线获取
                val book = Book(
                    id = bookId,
                    title = "",
                    sourceId = sourceId,
                    sourceUrl = sourceUrl
                )
                val onlineChapters = repository.getChapters(book)
                repository.cacheChapters(bookId, onlineChapters)
                chapters = onlineChapters.map { c ->
                    ChapterEntity(bookId = bookId, title = c.title, url = c.url, index = c.index)
                }
            }
            _uiState.value = _uiState.value.copy(chapters = chapters)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "获取章节失败: ${e.localizedMessage}")
        }
    }

    fun refreshChapters() {
        val book = _uiState.value.book ?: return
        val bookId = _uiState.value.bookId
        viewModelScope.launch {
            try {
                val onlineChapters = repository.getChapters(book)
                repository.cacheChapters(bookId, onlineChapters)
                _uiState.value = _uiState.value.copy(
                    chapters = onlineChapters.map { c ->
                        ChapterEntity(bookId = bookId, title = c.title, url = c.url, index = c.index)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "刷新失败: ${e.localizedMessage}")
            }
        }
    }
}

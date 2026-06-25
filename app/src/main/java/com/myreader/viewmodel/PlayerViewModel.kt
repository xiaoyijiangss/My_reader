package com.myreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.data.repository.BookRepository
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.PlayerState
import com.myreader.model.PlayerStatus
import com.myreader.player.AudioPlayer
import com.myreader.player.AudioPlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val audioPlayer: AudioPlayer by lazy {
        if (!AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player = AudioPlayer(application)
        }
        AudioPlayerHolder.player
    }

    private val repository = BookRepository()

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    fun loadBook(book: Book, chapterList: List<Chapter>, startIndex: Int = 0) {
        _currentBook.value = book
        _chapters.value = chapterList
        _currentChapterIndex.value = startIndex
        playChapter(startIndex)
    }

    fun playChapter(index: Int) {
        if (index !in _chapters.value.indices) return
        _currentChapterIndex.value = index
        val chapter = _chapters.value[index]
        val book = _currentBook.value ?: return

        viewModelScope.launch {
            try {
                val audioUrl = repository.getAudioUrl(chapter, book.sourceId)
                if (audioUrl != null) {
                    audioPlayer.play(audioUrl)
                    // 保存进度
                    repository.updateProgress(book.id, index, 0L)
                }
            } catch (e: Exception) {
                // 播放失败
            }
        }
    }

    fun nextChapter() {
        val next = _currentChapterIndex.value + 1
        if (next < _chapters.value.size) {
            playChapter(next)
        }
    }

    fun previousChapter() {
        val prev = _currentChapterIndex.value - 1
        if (prev >= 0) {
            playChapter(prev)
        }
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    fun skipForward() = audioPlayer.skipForward()

    fun skipBackward() = audioPlayer.skipBackward()

    fun seekTo(position: Long) = audioPlayer.seekTo(position)

    fun setSpeed(speed: Float) = audioPlayer.setSpeed(speed)

    fun getPlayerState(): StateFlow<PlayerState> = audioPlayer.state

    // ---- 定时关闭 ----
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                if (isActive) {
                    audioPlayer.stop()
                    _sleepTimerMinutes.value = 0
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = 0
    }

    fun handleChapterComplete() {
        // 自动播放下一章
        if (_currentChapterIndex.value + 1 < _chapters.value.size) {
            nextChapter()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        // 不停放播放器，让Service管理生命周期
    }
}

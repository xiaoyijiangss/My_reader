package com.myreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.data.AppPreferences
import com.myreader.data.repository.BookRepository
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.PlayerState
import com.myreader.model.PlayerStatus
import com.myreader.player.AudioPlayer
import com.myreader.player.AudioPlayerHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val _sleepTimerRemaining = MutableStateFlow("")
    val sleepTimerRemaining: StateFlow<String> = _sleepTimerRemaining.asStateFlow()

    /** 快进步秒 (后退, 前进) */
    private val _seekStepSeconds = MutableStateFlow(Pair(15, 30))
    val seekStepSeconds: StateFlow<Pair<Int, Int>> = _seekStepSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var sleepCountdownJob: Job? = null

    init {
        // 加载偏好设置
        viewModelScope.launch {
            try {
                val backward = AppPreferences.skipBackwardSeconds.first()
                val forward = AppPreferences.skipForwardSeconds.first()
                _seekStepSeconds.value = Pair(backward, forward)

                val speed = AppPreferences.playbackSpeed.first()
                audioPlayer.setSpeed(speed)

                val timer = AppPreferences.sleepTimerMinutes.first()
                if (timer > 0) setSleepTimer(timer)
            } catch (_: Exception) {}
        }
    }

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

    fun skipForward() {
        audioPlayer.skipForward(_seekStepSeconds.value.second * 1000L)
    }

    fun skipBackward() {
        audioPlayer.skipBackward(_seekStepSeconds.value.first * 1000L)
    }

    fun seekTo(position: Long) = audioPlayer.seekTo(position)

    fun setSpeed(speed: Float) {
        audioPlayer.setSpeed(speed)
        viewModelScope.launch { AppPreferences.setPlaybackSpeed(speed) }
    }

    fun getPlayerState(): StateFlow<PlayerState> = audioPlayer.state

    // ---- 定时关闭 ----
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        sleepCountdownJob?.cancel()

        if (minutes > 0) {
            val totalMs = minutes * 60_000L
            val startTime = System.currentTimeMillis()
            val endTime = startTime + totalMs

            // 倒计时更新
            sleepCountdownJob = viewModelScope.launch {
                while (isActive) {
                    val remaining = endTime - System.currentTimeMillis()
                    if (remaining <= 0) break
                    val remMin = remaining / 60_000
                    val remSec = (remaining % 60_000) / 1000
                    _sleepTimerRemaining.value = "%d:%02d".format(remMin, remSec)
                    delay(1000L)
                }
            }

            // 定时关闭
            sleepTimerJob = viewModelScope.launch {
                delay(totalMs)
                if (isActive) {
                    audioPlayer.stop()
                    _sleepTimerMinutes.value = 0
                    _sleepTimerRemaining.value = ""
                }
            }

            // 保存偏好
            viewModelScope.launch { AppPreferences.setSleepTimerMinutes(minutes) }
        } else {
            _sleepTimerRemaining.value = ""
            viewModelScope.launch { AppPreferences.setSleepTimerMinutes(0) }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepCountdownJob?.cancel()
        _sleepTimerMinutes.value = 0
        _sleepTimerRemaining.value = ""
        viewModelScope.launch { AppPreferences.setSleepTimerMinutes(0) }
    }

    fun handleChapterComplete() {
        viewModelScope.launch {
            val autoPlay = try { AppPreferences.autoPlayNext.first() } catch (_: Exception) { true }
            if (autoPlay && _currentChapterIndex.value + 1 < _chapters.value.size) {
                nextChapter()
            }
        }
    }

    fun updateSeekSteps(backwardSec: Int, forwardSec: Int) {
        _seekStepSeconds.value = Pair(backwardSec, forwardSec)
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        sleepCountdownJob?.cancel()
    }
}

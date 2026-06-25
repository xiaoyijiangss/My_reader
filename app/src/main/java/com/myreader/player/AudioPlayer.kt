package com.myreader.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 音频播放器封装
 * 基于 Media3 ExoPlayer，管理播放状态
 */
class AudioPlayer(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(com.myreader.model.PlayerState())
    val state: StateFlow<com.myreader.model.PlayerState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(
                    status = when (playbackState) {
                        Player.STATE_BUFFERING -> com.myreader.model.PlayerStatus.BUFFERING
                        Player.STATE_READY -> if (player.playWhenReady)
                            com.myreader.model.PlayerStatus.PLAYING
                        else com.myreader.model.PlayerStatus.PAUSED
                        Player.STATE_ENDED -> com.myreader.model.PlayerStatus.COMPLETED
                        else -> com.myreader.model.PlayerStatus.IDLE
                    },
                    duration = player.duration.coerceAtLeast(0L),
                    position = player.currentPosition.coerceAtLeast(0L)
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player.playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(
                        status = if (isPlaying) com.myreader.model.PlayerStatus.PLAYING
                        else com.myreader.model.PlayerStatus.PAUSED
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.value = "播放失败: ${error.localizedMessage}"
            }
        })
    }

    fun play(url: String) {
        _error.value = null
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipForward(ms: Long = 15_000L) {
        val newPos = (player.currentPosition + ms).coerceAtMost(player.duration)
        player.seekTo(newPos)
    }

    fun skipBackward(ms: Long = 15_000L) {
        val newPos = (player.currentPosition - ms).coerceAtLeast(0L)
        player.seekTo(newPos)
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun stop() {
        player.stop()
        _state.value = com.myreader.model.PlayerState(status = com.myreader.model.PlayerStatus.IDLE)
    }

    fun release() {
        player.release()
    }
}

package com.myreader.player

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.myreader.MainActivity

/**
 * 媒体播放前台服务
 * 确保后台播放不被打断，并显示通知栏控制
 */
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setupMediaSession()
    }

    private fun setupMediaSession() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player = AudioPlayer(this)
        }

        mediaSession = MediaSession.Builder(this, AudioPlayerHolder.player.player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 用户移除任务时不停止播放
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        super.onDestroy()
    }
}

/** 全局播放器持有者 */
object AudioPlayerHolder {
    lateinit var player: AudioPlayer
    val isPlayerInitialized: Boolean get() = ::player.isInitialized
}

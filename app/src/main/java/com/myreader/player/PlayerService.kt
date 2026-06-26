package com.myreader.player

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.myreader.MainActivity
import com.myreader.R

/**
 * 媒体播放前台服务
 * 确保后台播放不被打断，显示通知栏媒体控制（播放/暂停/快进/快退/上一章/下一章）
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

        // 构建自定义命令：快进30s、快退15s
        val forwardIntent = PendingIntent.getBroadcast(
            this, 100,
            Intent(ACTION_SKIP_FORWARD).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rewindIntent = PendingIntent.getBroadcast(
            this, 101,
            Intent(ACTION_SKIP_BACKWARD).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, AudioPlayerHolder.player.player)
            .setSessionActivity(pendingIntent)
            // 使用 Media3 默认通知，自动显示媒体控件
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

    companion object {
        const val ACTION_SKIP_FORWARD = "com.myreader.ACTION_SKIP_FORWARD"
        const val ACTION_SKIP_BACKWARD = "com.myreader.ACTION_SKIP_BACKWARD"
    }
}

/** 全局播放器持有者 */
object AudioPlayerHolder {
    lateinit var player: AudioPlayer
    val isPlayerInitialized: Boolean get() = ::player.isInitialized
}

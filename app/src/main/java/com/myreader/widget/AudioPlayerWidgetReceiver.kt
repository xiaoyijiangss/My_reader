package com.myreader.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.myreader.MainActivity
import com.myreader.R
import com.myreader.player.AudioPlayerHolder
import com.myreader.model.PlayerStatus

class AudioPlayerWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.myreader.widget.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.myreader.widget.PREVIOUS"
        const val ACTION_REWIND = "com.myreader.widget.REWIND"
        const val ACTION_FORWARD = "com.myreader.widget.FORWARD"
        const val ACTION_NEXT = "com.myreader.widget.NEXT"

        fun updateWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AudioPlayerWidgetReceiver::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.audio_player_widget)

            val player = if (AudioPlayerHolder.isPlayerInitialized) AudioPlayerHolder.player else null
            val state = player?.state?.value
            val isPlaying = state?.status == PlayerStatus.PLAYING

            // Title
            views.setTextViewText(R.id.widget_title, state?.book?.title ?: "MyReader")
            views.setTextViewText(R.id.widget_subtitle, state?.chapter?.title ?: "暂无播放内容")

            // Time
            if (state != null && state.duration > 0) {
                views.setTextViewText(R.id.widget_time_current, formatTime(state.position))
                views.setTextViewText(R.id.widget_time_total, formatTime(state.duration))
            } else {
                views.setTextViewText(R.id.widget_time_current, "--:--")
                views.setTextViewText(R.id.widget_time_total, "--:--")
            }

            // Play/Pause icon
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            // Set click listeners via PendingIntents
            val playPauseIntent = Intent(context, AudioPlayerWidgetReceiver::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                PendingIntent.getBroadcast(context, widgetId * 10 + 0, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val prevIntent = Intent(context, AudioPlayerWidgetReceiver::class.java).apply {
                action = ACTION_PREVIOUS
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_previous,
                PendingIntent.getBroadcast(context, widgetId * 10 + 1, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val rewindIntent = Intent(context, AudioPlayerWidgetReceiver::class.java).apply {
                action = ACTION_REWIND
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_rewind,
                PendingIntent.getBroadcast(context, widgetId * 10 + 2, rewindIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val forwardIntent = Intent(context, AudioPlayerWidgetReceiver::class.java).apply {
                action = ACTION_FORWARD
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_forward,
                PendingIntent.getBroadcast(context, widgetId * 10 + 3, forwardIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val nextIntent = Intent(context, AudioPlayerWidgetReceiver::class.java).apply {
                action = ACTION_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(context, widgetId * 10 + 4, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Click on the whole widget opens the app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, widgetId * 10 + 5, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun formatTime(millis: Long): String {
            val s = millis / 1000
            return "%02d:%02d".format(s / 60, s % 60)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                if (AudioPlayerHolder.isPlayerInitialized) {
                    AudioPlayerHolder.player.togglePlayPause()
                }
                updateWidgets(context)
            }
            ACTION_PREVIOUS -> {
                if (AudioPlayerHolder.isPlayerInitialized) {
                    AudioPlayerHolder.player.skipBackward(30_000L)
                }
                updateWidgets(context)
            }
            ACTION_REWIND -> {
                if (AudioPlayerHolder.isPlayerInitialized) {
                    AudioPlayerHolder.player.skipBackward(15_000L)
                }
                updateWidgets(context)
            }
            ACTION_FORWARD -> {
                if (AudioPlayerHolder.isPlayerInitialized) {
                    AudioPlayerHolder.player.skipForward(30_000L)
                }
                updateWidgets(context)
            }
            ACTION_NEXT -> {
                if (AudioPlayerHolder.isPlayerInitialized) {
                    AudioPlayerHolder.player.skipForward(15_000L)
                }
                updateWidgets(context)
            }
        }
    }
}

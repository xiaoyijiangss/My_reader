package com.myreader.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import com.myreader.MainActivity
import com.myreader.player.AudioPlayerHolder
import com.myreader.model.PlayerStatus

/**
 * Audio Player App Widget using Jetpack Glance
 * Shows current playback info and provides basic controls on the home screen.
 */
class AudioPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AudioPlayerWidget()
}

class AudioPlayerWidget : GlanceAppWidget() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val player = if (AudioPlayerHolder.isPlayerInitialized) AudioPlayerHolder.player else null
        val state = player?.state?.value
        val isPlaying = state?.status == PlayerStatus.PLAYING

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(0xE6FFFFFF.toInt()))
                .padding(12.dp)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Title + Play/Pause row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Title area
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = state?.book?.title ?: "MyReader",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(0xFF1A1A1A.toInt())
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = state?.chapter?.title ?: "暂无播放内容",
                        style = TextStyle(
                            color = ColorProvider(0xFF666666.toInt())
                        ),
                        maxLines = 1
                    )
                }

                // Play/Pause button
                Image(
                    provider = ImageProvider(
                        if (isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    ),
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = GlanceModifier
                        .size(40.dp)
                        .clickable(actionRunCallback<PlayPauseCallback>())
                )
            }

            if (state != null && state.duration > 0) {
                Spacer(modifier = GlanceModifier.height(8.dp))

                // Time row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = formatTime(state.position),
                        style = TextStyle(color = ColorProvider(0xFF999999.toInt()))
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = formatTime(state.duration),
                        style = TextStyle(color = ColorProvider(0xFF999999.toInt()))
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Controls row — evenly distributed via defaultWeight()
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_previous),
                        contentDescription = "上一章",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .size(32.dp)
                            .clickable(actionRunCallback<PreviousCallback>())
                    )
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_rew),
                        contentDescription = "后退15秒",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .size(32.dp)
                            .clickable(actionRunCallback<RewindCallback>())
                    )
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_ff),
                        contentDescription = "前进30秒",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .size(32.dp)
                            .clickable(actionRunCallback<ForwardCallback>())
                    )
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_next),
                        contentDescription = "下一章",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .size(32.dp)
                            .clickable(actionRunCallback<NextCallback>())
                    )
                }
            }
        }
    }
}

// ===== Action Callbacks =====

class PlayPauseCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player.togglePlayPause()
        }
        AudioPlayerWidget().update(context, glanceId)
    }
}

class PreviousCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player.skipBackward(30_000L)
        }
        AudioPlayerWidget().update(context, glanceId)
    }
}

class RewindCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player.skipBackward(15_000L)
        }
        AudioPlayerWidget().update(context, glanceId)
    }
}

class ForwardCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player.skipForward(30_000L)
        }
        AudioPlayerWidget().update(context, glanceId)
    }
}

class NextCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AudioPlayerHolder.isPlayerInitialized) {
            AudioPlayerHolder.player.skipForward(15_000L)
        }
        AudioPlayerWidget().update(context, glanceId)
    }
}

private fun formatTime(millis: Long): String {
    val s = millis / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

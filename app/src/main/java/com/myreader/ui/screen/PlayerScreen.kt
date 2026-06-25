package com.myreader.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.myreader.model.PlayerStatus
import com.myreader.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val book by viewModel.currentBook.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentIndex by viewModel.currentChapterIndex.collectAsState()
    val playerState by viewModel.getPlayerState().collectAsState()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsState()
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // 播放完成自动下一章
    LaunchedEffect(playerState.status) {
        if (playerState.status == PlayerStatus.COMPLETED) {
            viewModel.handleChapterComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("正在播放", style = MaterialTheme.typography.titleMedium)
            // 占位保持居中
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 封面
        AsyncImage(
            model = book?.coverUrl ?: "",
            contentDescription = book?.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 32.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 书名和章节
        Text(
            book?.title ?: "",
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            chapters.getOrNull(currentIndex)?.title ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 进度条
        Slider(
            value = if (playerState.duration > 0)
                playerState.position.toFloat() / playerState.duration.toFloat()
            else 0f,
            onValueChange = { fraction ->
                val pos = (fraction * playerState.duration).toLong()
                viewModel.seekTo(pos)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(playerState.position), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(playerState.duration), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 倍速
            TextButton(onClick = { showSpeedDialog = true }) {
                Text("${playerState.speed}x")
            }

            // 上一章
            IconButton(onClick = { viewModel.previousChapter() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一章", modifier = Modifier.size(36.dp))
            }

            // 后退15s
            IconButton(onClick = { viewModel.skipBackward() }) {
                Icon(Icons.Default.Replay10, contentDescription = "后退15秒", modifier = Modifier.size(32.dp))
            }

            // 播放/暂停
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (playerState.status == PlayerStatus.PLAYING) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                    modifier = Modifier.size(36.dp)
                )
            }

            // 前进15s
            IconButton(onClick = { viewModel.skipForward() }) {
                Icon(Icons.Default.Forward30, contentDescription = "前进15秒", modifier = Modifier.size(32.dp))
            }

            // 下一章
            IconButton(onClick = { viewModel.nextChapter() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一章", modifier = Modifier.size(36.dp))
            }

            // 定时
            TextButton(onClick = { showSleepTimerDialog = true }) {
                Text(if (sleepTimer > 0) "${sleepTimer}分" else "定时")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 章节列表
        if (chapters.isNotEmpty()) {
            Text(
                "目录",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(chapters) { index, chapter ->
                    val isCurrent = index == currentIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { viewModel.playChapter(index) },
                        colors = if (isCurrent)
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                    ) {
                        Text(
                            chapter.title,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 倍速选择对话框
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("播放速度") },
            text = {
                Column {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = playerState.speed == speed,
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${speed}x")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 定时关闭对话框
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("定时关闭") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 15, 30, 45, 60).forEach { minutes ->
                            FilterChip(
                                selected = sleepTimer == minutes,
                                onClick = {
                                    viewModel.setSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                },
                                label = { Text(if (minutes == 0) "关闭" else "${minutes}分") }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)

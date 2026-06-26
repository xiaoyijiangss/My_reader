package com.myreader.ui.screen

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val seekStep by viewModel.seekStepSeconds.collectAsState()
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }

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
            // 睡眠定时指示器
            if (sleepTimer > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable { showSleepTimerDialog = true }
                ) {
                    Text(
                        "⏰ ${sleepRemaining}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 封面
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = book?.coverUrl ?: "",
                contentDescription = book?.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 书名和章节
        Text(
            book?.title ?: "",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            chapters.getOrNull(currentIndex)?.title ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条
        Slider(
            value = if (playerState.duration > 0)
                playerState.position.toFloat() / playerState.duration.toFloat()
            else 0f,
            onValueChange = { fraction ->
                val pos = (fraction * playerState.duration).toLong()
                viewModel.seekTo(pos)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        // 时间
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(playerState.position), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(playerState.duration), style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 主要播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 倍速
            FilledTonalButton(onClick = { showSpeedDialog = true }) {
                Text("${playerState.speed}x")
            }

            // 上一章
            IconButton(onClick = { viewModel.previousChapter() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一章", modifier = Modifier.size(36.dp))
            }

            // 后退
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.skipBackward() }) {
                    Icon(Icons.Default.Replay10, contentDescription = "后退", modifier = Modifier.size(28.dp))
                }
                Text("${seekStep.first}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 播放/暂停
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    if (playerState.status == PlayerStatus.PLAYING) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                    modifier = Modifier.size(40.dp)
                )
            }

            // 前进
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.skipForward() }) {
                    Icon(Icons.Default.Forward30, contentDescription = "前进", modifier = Modifier.size(28.dp))
                }
                Text("${seekStep.second}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 下一章
            IconButton(onClick = { viewModel.nextChapter() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一章", modifier = Modifier.size(36.dp))
            }

            // 定时
            FilledTonalButton(onClick = { showSleepTimerDialog = true }) {
                Text(if (sleepTimer > 0) "定时中" else "定时")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 底部功能区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { showChapterList = !showChapterList }) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("目录")
            }
            TextButton(onClick = { viewModel.seekTo(0L) }) {
                Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("重新播放")
            }
        }

        // 章节列表
        if (showChapterList && chapters.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(chapters) { index, chapter ->
                    val isCurrent = index == currentIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .clickable {
                                viewModel.playChapter(index)
                                showChapterList = false
                            },
                        color = if (isCurrent)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCurrent) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                chapter.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.speed == speed,
                                onClick = { viewModel.setSpeed(speed); showSpeedDialog = false }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${speed}x", style = MaterialTheme.typography.bodyLarge)
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
                    // 快捷选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(0 to "关闭", 10 to "10分", 15 to "15分", 30 to "30分", 45 to "45分", 60 to "60分")
                        options.forEach { (minutes, label) ->
                            FilterChip(
                                selected = sleepTimer == minutes,
                                onClick = {
                                    viewModel.setSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 自定义分钟
                    var customMinutes by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("自定义(分钟)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val mins = customMinutes.toIntOrNull() ?: 0
                            if (mins > 0) {
                                viewModel.setSleepTimer(mins)
                                showSleepTimerDialog = false
                            }
                        }) {
                            Text("确定")
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
    val hours = minutes / 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes % 60, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

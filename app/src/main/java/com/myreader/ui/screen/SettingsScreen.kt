package com.myreader.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myreader.data.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageSources: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // 播放设置状态
    var skipBackward by remember { mutableIntStateOf(AppPreferences.DEFAULT_SKIP_BACKWARD) }
    var skipForward by remember { mutableIntStateOf(AppPreferences.DEFAULT_SKIP_FORWARD) }
    var autoPlayNext by remember { mutableStateOf(AppPreferences.DEFAULT_AUTO_PLAY_NEXT) }

    // 加载保存的设置
    LaunchedEffect(Unit) {
        try {
            skipBackward = AppPreferences.skipBackwardSeconds.first()
            skipForward = AppPreferences.skipForwardSeconds.first()
            autoPlayNext = AppPreferences.autoPlayNext.first()
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ========== 播放设置 ==========
            item {
                Text(
                    "播放设置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 快退时长
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("快退时长: ${skipBackward}秒", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("点击播放器倒退按钮时跳转的秒数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30, 60).forEach { sec ->
                                FilterChip(
                                    selected = skipBackward == sec,
                                    onClick = {
                                        skipBackward = sec
                                        scope.launch { AppPreferences.setSkipBackwardSeconds(sec) }
                                    },
                                    label = { Text("${sec}s", fontSize = 13.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 快进时长
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("快进时长: ${skipForward}秒", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("点击播放器前进按钮时跳转的秒数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 15, 30, 60, 120).forEach { sec ->
                                FilterChip(
                                    selected = skipForward == sec,
                                    onClick = {
                                        skipForward = sec
                                        scope.launch { AppPreferences.setSkipForwardSeconds(sec) }
                                    },
                                    label = { Text("${sec}s", fontSize = 13.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 自动播放下一章
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自动播放下一章", style = MaterialTheme.typography.titleSmall)
                            Text("当前章节播放完成后自动切换", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = {
                                autoPlayNext = it
                                scope.launch { AppPreferences.setAutoPlayNext(it) }
                            }
                        )
                    }
                }
            }

            // ========== 书源管理 ==========
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "书源管理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onManageSources
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("书源管理", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "导入/管理 Legado 格式书源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ========== 关于 ==========
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "关于",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MyReader 有声书阅读器", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "功能说明：\n" +
                                "• 全网搜索有声书资源\n" +
                                "• 支持 Legado/「阅读」JSON 书源\n" +
                                "• 多书源聚合搜索、CSS爬虫引擎\n" +
                                "• 后台播放 + 通知栏控制\n" +
                                "• 倍速播放（0.5x ~ 3.0x）\n" +
                                "• 定时关闭（支持自定义时长）\n" +
                                "• 快进快退（可调节步长）\n" +
                                "• 自动记录播放进度\n" +
                                "• 桌面小组件控制播放\n" +
                                "• 「我的听书」社区书源索引\n\n" +
                                "使用教程：\n" +
                                "1. 进入「设置」→「书源管理」\n" +
                                "2. 点「一键获取」下载预置书源\n" +
                                "3. 或手动粘贴 Legado 格式的 JSON 书源\n" +
                                "4. 回到搜索页搜索有声小说\n" +
                                "5. 长按桌面空白处添加播放小组件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 免责声明
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("免责声明", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "本应用仅供个人学习和技术研究使用。\n所有内容来源于网络公开资源，版权归原作者所有。\n请勿用于商业用途。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

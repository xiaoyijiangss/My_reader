package com.myreader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageSources: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // 书源管理入口
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onManageSources
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                    Text(
                        "→",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("关于 MyReader", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "一个开源的个人听书工具\n\n" +
                            "功能说明：\n" +
                            "• 全网搜索有声书资源\n" +
                            "• 支持「阅读」APP Legado JSON 书源\n" +
                            "• 支持多书源聚合搜索\n" +
                            "• 后台播放 + 通知栏控制\n" +
                            "• 倍速播放（0.75x ~ 2.0x）\n" +
                            "• 定时关闭\n" +
                            "• 自动记录播放进度\n" +
                            "• 一键从社区仓库获取书源\n\n" +
                            "使用教程：\n" +
                            "1. 进入「书源管理」\n" +
                            "2. 点「一键获取」下载社区书源\n" +
                            "3. 或手动粘贴「阅读」格式的JSON书源\n" +
                            "4. 回到搜索页即可搜索",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("免责声明", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本应用仅供个人学习和技术研究使用。\n" +
                            "所有内容来源于网络公开资源，版权归原作者所有。\n" +
                            "请勿用于商业用途。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

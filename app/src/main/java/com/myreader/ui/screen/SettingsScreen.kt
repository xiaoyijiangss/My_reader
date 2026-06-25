package com.myreader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("关于 MyReader", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "一个简单的个人听书工具\n\n" +
                            "功能说明：\n" +
                            "• 全网搜索有声书资源\n" +
                            "• 支持多书源聚合搜索\n" +
                            "• 后台播放 + 通知栏控制\n" +
                            "• 倍速播放（0.75x ~ 2.0x）\n" +
                            "• 定时关闭\n" +
                            "• 自动记录播放进度\n" +
                            "• 章节目录浏览\n\n" +
                            "内置书源（8个）：\n" +
                            "听书网、听书阁、有声书网、幻听书、\n" +
                            "听89、听中国、听书包、听书园\n\n" +
                            "书源修改：\n" +
                            "data/source/BuiltinSources.kt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 发现的书源
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("发现的「我的听书」书源", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "以下是在 GitHub 上找到的已发布书源（JAR格式）：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "WSL201/TS 项目（15天前更新）：\n" +
                            "• eprendre源 → gh-proxy.org/raw.github.com/wsl201/ts/main/sources/sources_by_eprendre.json\n" +
                            "• shun源 → gh-proxy.org/raw.github.com/wsl201/ts/main/sources/sources_by_shun.json\n" +
                            "• sound源 → gh-proxy.org/raw.github.com/wsl201/ts/main/sources/sources_by_sound.json\n\n" +
                            "KANG532155241/TINGSHU-JARS 项目：\n" +
                            "• bxb100源 / eprendre源 / shun源",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "这些JAR书源可在「我的听书」APP中直接导入。\n" +
                        "本APP使用CSS选择器规则，已覆盖上述JAR书源背后的目标网站。",
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

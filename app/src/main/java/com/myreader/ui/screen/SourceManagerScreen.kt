package com.myreader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myreader.data.source.SourceHubClient
import com.myreader.viewmodel.SourceManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagerScreen(
    onBack: () -> Unit,
    viewModel: SourceManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var showHubDialog by remember { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("书源管理") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // 顶部操作按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("导入JSON")
            }
            Button(
                onClick = { showHubDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("社区仓库")
            }
        }

        // 一键获取按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.fetchAllHubs() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isFetching
            ) {
                if (uiState.isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isFetching) uiState.fetchProgress else "一键获取")
            }
            OutlinedButton(
                onClick = { viewModel.reloadPrebuilt() },
                enabled = !uiState.isFetching
            ) {
                Text("加载预置源")
            }
        }

        // 状态消息
        if (uiState.importResult.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    uiState.importResult,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        uiState.error!!,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 书源数量统计
        Text(
            "已加载 ${uiState.sources.size} 个 Legado 书源",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 空态
        if (uiState.sources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有 Legado 书源",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击「加载预置源」使用内置的有声书源\n" +
                            "或点击「社区仓库」获取第三方书源\n" +
                            "或粘贴「阅读」APP 的 JSON 导入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "💡 提示：\n" +
                            "• 主流听书网站有反爬保护，部分源可能搜索不到\n" +
                            "• 获取最新书源请访问 yckceo.com 搜索「听书」\n" +
                            "• 推荐下载「懒人听书」「听书网」等标记「声」的源\n" +
                            "• 导入后可在搜索页搜索有声小说",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 书源列表
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(uiState.sources) { source ->
                SourceItem(
                    source = source,
                    onToggle = { viewModel.toggleSource(source) },
                    onDelete = { viewModel.removeSource(source) }
                )
            }
        }
    }

    // ---- 导入JSON对话框 ----
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入书源JSON") },
            text = {
                Column {
                    Text(
                        "粘贴「阅读」APP 书源的 JSON 内容：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        placeholder = { Text("[\n  {\"bookSourceUrl\": \"...\"}\n]") },
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "也支持输入书源文件的 URL：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://raw.githubusercontent.com/...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            viewModel.importFromJson(jsonText)
                        }
                        if (urlText.isNotBlank()) {
                            viewModel.importFromUrl(urlText)
                        }
                        showImportDialog = false
                        jsonText = ""
                        urlText = ""
                    },
                    enabled = !uiState.isImporting
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ---- 社区仓库对话框 ----
    if (showHubDialog) {
        AlertDialog(
            onDismissRequest = { showHubDialog = false },
            title = { Text("社区书源仓库") },
            text = {
                Column {
                    Text(
                        "ⓘ 标「Legado」的为可用的「阅读」格式源\n  标「JAR」的为「我的听书」插件（仅供索引参考）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Legado 源
                    Text("Legado 格式源（可直接导入）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    SourceHubClient.KNOWN_HUBS.filter { it.type == SourceHubClient.HubType.LEGADO_JSON }.forEach { hub ->
                        SourceHubItem(hub) {
                            viewModel.fetchFromHub(hub)
                            showHubDialog = false
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    // 「我的听书」JAR 索引
                    Text("「我的听书」JAR 插件索引（仅供参考）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    SourceHubClient.KNOWN_HUBS.filter { it.type == SourceHubClient.HubType.MY_TINGSHU_JAR }.take(5).forEach { hub ->
                        SourceHubItem(hub) { /* JAR源不可直接导入 */ }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.fetchAllHubs()
                    showHubDialog = false
                }) {
                    Text("一键获取 Legado 源")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHubDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SourceHubItem(
    hub: SourceHubClient.SourceHub,
    onClick: () -> Unit
) {
    val typeTag = when (hub.type) {
        SourceHubClient.HubType.LEGADO_JSON -> "Legado"
        SourceHubClient.HubType.MY_TINGSHU_JAR -> "JAR"
    }
    val tagColor = when (hub.type) {
        SourceHubClient.HubType.LEGADO_JSON -> MaterialTheme.colorScheme.primary
        SourceHubClient.HubType.MY_TINGSHU_JAR -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(hub.name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = tagColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            typeTag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = tagColor
                        )
                    }
                }
                Text(
                    hub.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SourceItem(
    source: com.myreader.data.source.LegadoSource,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    source.bookSourceName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    source.bookSourceUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (source.bookSourceGroup.isNotBlank()) {
                    Text(
                        source.bookSourceGroup,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(
                checked = source.enabled,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

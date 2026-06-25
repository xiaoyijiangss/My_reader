package com.myreader.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.myreader.model.Book
import com.myreader.model.Chapter
import com.myreader.model.SearchResult
import com.myreader.viewmodel.BookDetailViewModel
import com.myreader.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    isFromLib: Boolean,
    incomingSourceId: String,
    incomingSourceUrl: String,
    onPlay: (Book, List<Chapter>, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastReadIndex = uiState.book?.lastReadChapterIndex ?: 0

    LaunchedEffect(bookId) {
        if (isFromLib && bookId > 0) {
            viewModel.loadFromBookId(bookId)
        } else if (incomingSourceId.isNotBlank() && incomingSourceUrl.isNotBlank()) {
            viewModel.loadFromSearch(
                SearchResult(
                    title = "", author = "", coverUrl = "",
                    url = incomingSourceUrl, sourceId = incomingSourceId
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("书籍详情") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.book != null) {
            val book = uiState.book!!
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 书籍信息
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier.size(120.dp, 160.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                "作者: ${book.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!uiState.isAddedToLibrary) {
                                Button(onClick = { viewModel.addToLibrary() }) {
                                    Text("加入书架")
                                }
                            }
                        }
                    }
                }

                // 简介
                if (book.description.isNotBlank()) {
                    item {
                        Text("简介", style = MaterialTheme.typography.titleMedium)
                        Text(
                            book.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 章节列表标题 + 刷新
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目录 (${uiState.chapters.size}章)", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.refreshChapters() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }

                // 章节列表
                if (uiState.chapters.isNotEmpty()) {
                    itemsIndexed(uiState.chapters) { idx, chapter ->
                        val isLastRead = idx == lastReadIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val bookObj = Book(
                                        id = if (isFromLib) bookId else uiState.bookId,
                                        title = book.title,
                                        author = book.author,
                                        coverUrl = book.coverUrl,
                                        sourceId = book.sourceId,
                                        sourceUrl = book.sourceUrl
                                    )
                                    val chapters = uiState.chapters.map { entity ->
                                        Chapter(
                                            id = 0, bookId = uiState.bookId,
                                            title = entity.title, url = entity.url,
                                            index = entity.index
                                        )
                                    }
                                    playerViewModel.loadBook(bookObj, chapters, idx)
                                    onPlay(bookObj, chapters, idx)
                                },
                            colors = if (isLastRead)
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ) else CardDefaults.cardColors()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "播放",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

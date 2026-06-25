package com.myreader.model

data class Chapter(
    val id: Long = 0,
    val bookId: Long,
    val title: String,
    val url: String = "",
    val index: Int = 0,
    val duration: Long = 0L,
    val isDownloaded: Boolean = false,
    val localPath: String = ""
)

package com.myreader.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "未知",
    val coverUrl: String = "",
    val description: String = "",
    val sourceId: String = "",
    val sourceUrl: String = "",
    val category: String = "",
    val isFavorited: Boolean = false,
    val lastReadChapterIndex: Int = -1,
    val lastReadPosition: Long = 0L,
    val addTime: Long = System.currentTimeMillis()
)

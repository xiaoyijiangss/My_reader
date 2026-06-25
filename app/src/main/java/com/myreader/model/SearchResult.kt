package com.myreader.model

data class SearchResult(
    val title: String,
    val author: String,
    val coverUrl: String,
    val url: String,
    val sourceId: String,
    val description: String = ""
)

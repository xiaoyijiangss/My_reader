package com.myreader.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["sourceId", "sourceUrl"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

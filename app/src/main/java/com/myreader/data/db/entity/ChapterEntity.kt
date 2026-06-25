package com.myreader.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chapters",
    primaryKeys = ["bookId", "index"],
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookId")]
)
data class ChapterEntity(
    val bookId: Long,
    val title: String,
    val url: String = "",
    val index: Int = 0,
    val duration: Long = 0L,
    val isDownloaded: Boolean = false,
    val localPath: String = ""
)

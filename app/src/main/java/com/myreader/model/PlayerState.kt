package com.myreader.model

enum class PlayerStatus { IDLE, BUFFERING, PLAYING, PAUSED, COMPLETED }

data class PlayerState(
    val book: Book? = null,
    val chapter: Chapter? = null,
    val status: PlayerStatus = PlayerStatus.IDLE,
    val position: Long = 0L,
    val duration: Long = 0L,
    val speed: Float = 1.0f,
    val chapterList: List<Chapter> = emptyList()
)

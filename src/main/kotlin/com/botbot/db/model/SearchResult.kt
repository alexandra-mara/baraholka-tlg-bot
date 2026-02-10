package com.botbot.db.model

import java.time.LocalDateTime

data class SearchResult(
    val chatId: Long,
    val chatTitle: String,
    val chatUsername: String?,
    val messageId: Long,
    val text: String,
    val senderName: String?,
    val timestamp: LocalDateTime
)

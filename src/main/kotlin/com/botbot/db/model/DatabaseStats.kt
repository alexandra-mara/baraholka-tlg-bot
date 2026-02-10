package com.botbot.db.model

import java.time.LocalDateTime

data class DatabaseStats(
    val totalMessages: Int = 0,
    val totalChats: Int = 0,
    val totalUsers: Int = 0,
    val oldestMessage: LocalDateTime? = null,
    val newestMessage: LocalDateTime? = null
)

package com.botbot.db.model

data class Subscription(
    val subscriptionId: Int,
    val userId: Long,
    val keyword: String
)

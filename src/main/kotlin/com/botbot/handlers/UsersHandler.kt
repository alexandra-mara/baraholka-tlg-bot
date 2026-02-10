package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode

suspend fun handleUsers(bot: Bot, message: Message, database: MessageDatabase) {
    val users = database.getAllUsers()

    if (users.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "⚠️ No users found in the database yet."
        )
    } else {
        val userList = users.joinToString("\n") { "- ${it.name} (${it.id})" }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "*Found ${users.size} users:*\n$userList",
            parseMode = ParseMode.MARKDOWN
        )
    }
}

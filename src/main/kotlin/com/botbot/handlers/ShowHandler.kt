package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.botbot.utils.createMessageLink
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

suspend fun handleShow(bot: Bot, message: Message, args: List<String>, database: MessageDatabase) {
    val limit = args.firstOrNull()?.toIntOrNull() ?: 10

    val results = database.showLastMessages(limit)

    if (results.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "⚠️ The database is empty. No messages to show."
        )
    } else {
        val response = results.joinToString("\n\n") { result ->
            val localTimestamp = result.timestamp.atZone(ZoneId.systemDefault())
            val formattedTimestamp = localTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val link = createMessageLink(result)
            
            """
            [$formattedTimestamp] *${result.chatTitle}* | ${result.senderName ?: "N/A"}
            ${result.text}
            [Go to message]($link)
            """.trimIndent()
        }

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "*Showing last ${results.size} messages:*\n\n$response",
            parseMode = ParseMode.MARKDOWN
        )
    }
}

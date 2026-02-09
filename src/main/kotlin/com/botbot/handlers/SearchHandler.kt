package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.botbot.services.getWordForms
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

suspend fun handleSearch(bot: Bot, message: Message, args: List<String>, database: MessageDatabase, monitoredChats: List<Long>) {
    if (database.getStats().totalMessages == 0) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "⚠️ The message database is empty. There is nothing to search yet."
        )
        return
    }

    val query = args.joinToString(" ")
    if (query.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "🔍 Usage: /search *tent*\n" +
                    "You can search for multiple words: /search *big tent*",
            parseMode = ParseMode.MARKDOWN
        )
        return
    }

    // Show "searching..."
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "🔎 Searching for word forms of \"$query\"..."
    )

    // Get word forms
    val wordForms = getWordForms(query)

    // Searching in the database
    val results = database.searchMessages(
        query = wordForms,
        chatIds = monitoredChats.takeIf { it.isNotEmpty() },
        daysBack = 7,
        limit = 10
    )

    // Forming the response
    if (results.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "😕 Nothing found for \"*$query*\" in the last week.",
            parseMode = ParseMode.MARKDOWN
        )
    } else {
        // Sending the first result with the count
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = """
            ✅ Found *${results.size}* listings for "*$query*":
            
            📝 *Examples:*
            ${results.take(3).joinToString("\n\n") { result ->
                "🏷️ ${result.chatTitle}\n" +
                        "👤 ${result.senderName ?: "Anonymous"}\n" +
                        "🕐 ${result.timestamp.format(dateFormatter)}\n" +
                        "💬 ${result.text.take(150)}${if (result.text.length > 150) "..." else ""}"
            }}
            
            ${if (results.size > 3) "📄 *And ${results.size - 3} more results...*" else ""}
            """.trimIndent(),
            parseMode = ParseMode.MARKDOWN
        )

        // Sending the rest of the results one by one
        results.drop(3).take(5).forEachIndexed { index, result ->
            delay(500) // To avoid flood limit

            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = """
                📌 Result ${index + 4}:
                Chat: ${result.chatTitle}
                From: ${result.senderName ?: "Anonymous"}
                Time: ${result.timestamp.format(dateFormatter)}
                
                ${result.text}
                """.trimIndent()
            )
        }
    }
}

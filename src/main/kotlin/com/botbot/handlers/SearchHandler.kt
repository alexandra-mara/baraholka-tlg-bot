package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.botbot.services.getWordForms
import com.botbot.utils.createMessageLink
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.delay
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Helper to escape Markdown characters in user-generated content
private fun String.escapeMarkdown(): String {
    return this.replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("`", "\\`")
}

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

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "🔎 Searching for an exact match of \"$query\"..."
    )

    val wordForms = listOf(query)

    val results = database.searchMessages(
        query = wordForms,
        chatIds = monitoredChats.takeIf { it.isNotEmpty() },
        daysBack = 7,
        limit = 10
    )

    if (results.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "😕 Nothing found for \"*$query*\" in the last week.",
            parseMode = ParseMode.MARKDOWN
        )
    } else {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = """
            ✅ Found *${results.size}* listings for "*$query*":
            
            📝 *Examples:*
            ${results.take(3).joinToString("\n\n") { result ->
                val localTimestamp = result.timestamp.atZone(ZoneId.systemDefault())
                val link = createMessageLink(result)
                val safeText = result.text.escapeMarkdown()
                "🏷️ ${result.chatTitle}\n" +
                        "👤 ${result.senderName ?: "Anonymous"}\n" +
                        "🕐 ${localTimestamp.format(dateFormatter)}\n" +
                        "💬 $safeText${if (result.text.length > 150) "..." else ""}\n" +
                        "[Go to message]($link)"
            }}
            
            ${if (results.size > 3) "📄 *And ${results.size - 3} more results...*" else ""}
            """.trimIndent(),
            parseMode = ParseMode.MARKDOWN
        )

        results.drop(3).take(5).forEachIndexed { index, result ->
            delay(500) // To avoid flood limit
            val link = createMessageLink(result)
            val safeText = result.text.escapeMarkdown()

            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = """
                📌 Result ${index + 4}:
                Chat: ${result.chatTitle}
                From: ${result.senderName ?: "Anonymous"}
                Time: ${result.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter)}
                
                $safeText
                [Go to message]($link)
                """.trimIndent(),
                parseMode = ParseMode.MARKDOWN
            )
        }
    }
}

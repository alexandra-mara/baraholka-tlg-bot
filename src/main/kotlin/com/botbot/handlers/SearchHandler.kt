package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.botbot.services.getWordForms
import com.botbot.utils.createMessageLink
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
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
        text = "🔎 Searching for \"$query\" and its word forms..."
    )

    // Check cache for word forms, or fetch if missing
    var wordForms = database.getWordForms(query)
    if (wordForms == null) {
        println("[Cache] Search: Missing forms for '$query'. Fetching...")
        wordForms = getWordForms(query)
        database.saveWordForms(query, wordForms)
    }

    val results = database.searchMessages(
        query = wordForms,
        chatIds = monitoredChats.takeIf { it.isNotEmpty() },
        daysBack = 7,
        limit = 10
    )

    val subscribeKeyboard = InlineKeyboardMarkup.create(
        listOf(listOf(InlineKeyboardButton.CallbackData("🔔 Subscribe to \"$query\"", "subscribe_query:$query")))
    )

    if (results.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "😕 Nothing found for \"*$query*\" in the last week.",
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = subscribeKeyboard
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
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = if (results.size <= 3) subscribeKeyboard else null
        )

        val remainingResults = results.drop(3).take(5)
        remainingResults.forEachIndexed { index, result ->
            delay(500) // To avoid flood limit
            val link = createMessageLink(result)
            val safeText = result.text.escapeMarkdown()

            // Put the subscription button on the very last result message
            val isLastMessage = index == remainingResults.size - 1

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
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = if (isLastMessage) subscribeKeyboard else null
            )
        }
    }
}

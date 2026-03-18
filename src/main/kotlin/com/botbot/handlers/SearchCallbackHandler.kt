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

suspend fun handleSearchCallback(bot: Bot, message: Message, args: List<String>, database: MessageDatabase, monitoredChats: List<Long>) {
    val user = message.from ?: return // We need the user to send the callback to
    val query = args.joinToString(" ")

    if (query.isEmpty()) {
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Usage: /search_callback [query]")
        return
    }

    // 1. Acknowledge the command in the original chat
    bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "🔎 Searching for \"$query\". Results will be sent to you in a private message.")

    // 2. Perform the search
    val wordForms = getWordForms(query)
    val results = database.searchMessages(
        query = if (wordForms.isNotEmpty()) wordForms else listOf(query),
        chatIds = monitoredChats.takeIf { it.isNotEmpty() },
        daysBack = 7,
        limit = 10
    )

    // 3. Prepare the results message
    val userChatId = ChatId.fromId(user.id)
    val responseText: String
    val keyboard: InlineKeyboardMarkup?

    if (results.isEmpty()) {
        responseText = "😕 Nothing found for \"$query\" in the last week."
        keyboard = InlineKeyboardMarkup.create(
            listOf(listOf(InlineKeyboardButton.CallbackData("🔔 Subscribe to \"$query\"", "subscribe_query:$query")))
        )
    } else {
        val response = results.joinToString("\n\n") { result ->
            val localTimestamp = result.timestamp.atZone(ZoneId.systemDefault())
            val formattedTimestamp = localTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val link = createMessageLink(result)
            val safeText = result.text.escapeMarkdown()

            """
            [$formattedTimestamp] *${result.chatTitle}* | ${result.senderName ?: "N/A"}
            $safeText
            [Go to message]($link)
            """.trimIndent()
        }
        responseText = "*Found ${results.size} results for \"$query\":*\n\n$response"
        keyboard = InlineKeyboardMarkup.create(
            listOf(listOf(InlineKeyboardButton.CallbackData("🔔 Subscribe to \"$query\"", "subscribe_query:$query")))
        )
    }

    // 4. Send the results
    bot.sendMessage(
        chatId = userChatId,
        text = responseText,
        parseMode = ParseMode.MARKDOWN,
        replyMarkup = keyboard
    ).fold({
        // Success!
    }, {
        val errorMessage = "⚠️ @${user.username ?: user.firstName}, I couldn't send you a private message. Please make sure you have started a chat with me first!"
        val log = "[Callback Error] Failed to send PM to ${user.id}: $it"
        println(log)
        File("full_activity.log").appendText("$log\n")
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = errorMessage)
    })
}

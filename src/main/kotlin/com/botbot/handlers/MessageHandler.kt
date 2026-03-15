package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.botbot.services.getWordForms
import com.botbot.utils.createMessageLink
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun handleMessage(bot: Bot, message: Message, database: MessageDatabase, monitoredChats: List<Long>) = coroutineScope {
    val chat = message.chat
    val chatId = chat.id
    val chatTitle = chat.title
    val chatUsername = chat.username
    val user = message.from
    val userId = user?.id
    val userName = user?.firstName
    val messageId = message.messageId
    val text = message.text ?: message.caption ?: "[Non-text message]"

    // --- Comprehensive Logging for ALL Messages ---
    val forwardedFrom = message.forwardFrom?.let { " | Forwarded from: ${it.firstName}" } ?: ""
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val logMessage = "[$timestamp] Chat:$chatId($chatTitle) | Msg:$messageId | User:$userId($userName)$forwardedFrom | Text: $text"

    println(logMessage)
    File("full_activity.log").appendText("$logMessage\n")
    // ----------------------------------------------

    // Always save the user who sent the message.
    user?.let { database.saveUser(it.id, it.firstName) }

    // --- Main Logic: Save Message & Dispatch Notifications ---
    if (chatId in monitoredChats) {
        // 1. Save the message to the database
        if (text.isNotBlank()) {
            database.saveMessage(
                chatId = chatId,
                chatTitle = chatTitle,
                chatUsername = chatUsername,
                messageId = message.messageId,
                text = text,
                senderName = userName,
                senderId = userId,
                forwardedFromId = message.forwardFrom?.id,
                forwardedFromName = message.forwardFrom?.firstName,
                timestamp = message.date
            )
        }

        // 2. Deconstruct message into searchable keywords
        val baseWords = text.split(" ").map { it.lowercase().trim() }.filter { it.length > 3 }.toSet()
        val allKeywordForms = baseWords.map { getWordForms(it) }.flatten().toSet()

        // 3. Find subscribers for these keywords
        val subscribers = database.findSubscribersForKeywords(allKeywordForms)

        // 4. Dispatch notifications
        if (subscribers.isNotEmpty()) {
            val log = "[Notification] Found subscribers for keywords: ${subscribers.keys.joinToString()}"
            println(log)
            File("full_activity.log").appendText("$log\n")

            subscribers.forEach { (keyword, userIds) ->
                userIds.forEach { subscriberId ->
                    // Don't notify the user about their own message
                    if (subscriberId == userId) return@forEach

                    launch {
                        val notificationText = "🔔 You have a new notification for the keyword '$keyword' in the chat *${chatTitle}*."
                        bot.sendMessage(chatId = ChatId.fromId(subscriberId), text = notificationText, parseMode = ParseMode.MARKDOWN).fold(
                            {
                                // Success
                            },
                            {
                                val errorLog = "[Notification Error] Failed to send PM to $subscriberId: $it"
                                println(errorLog)
                                File("full_activity.log").appendText("$errorLog\n")
                            }
                        )
                        delay(500) // Small delay to prevent rate-limiting
                    }
                }
            }
        }
    } else {
        // This is an unmonitored chat. Log its ID for discoverability.
        logUnmonitoredChat(chat, userId, userName)
    }
}

private fun logUnmonitoredChat(chat: com.github.kotlintelegrambot.entities.Chat, userId: Long?, userName: String?) {
    if (chat.type == "supergroup" || chat.type == "group" || chat.type == "private" || chat.type == "channel") {
        val logFile = File("chat_ids.log")
        val logFileContent = if (logFile.exists()) logFile.readText() else ""

        val hasBeenLogged = logFileContent.lines().any { it.startsWith("${chat.id}|") }

        if (!hasBeenLogged) {
            val effectiveTitle = chat.title ?: "Private Chat with ${userName ?: "user"}"
            val logTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            logFile.appendText("${chat.id}|$effectiveTitle|$logTimestamp\n")

            println("\n🎯 New unmonitored chat detected (logging to chat_ids.log):")
            println("   Type: ${chat.type}")
            println("   Title: $effectiveTitle")
            println("   ID: ${chat.id}")
            println("   (Add this ID to your Config.kt to start saving messages)")
        }
    }
}

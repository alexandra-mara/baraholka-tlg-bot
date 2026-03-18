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
        // 1. Save the message to the database (if it's not a command)
        if (text.isNotBlank() && !text.startsWith("/")) {
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

        // 2. Extract words from message and check against subscriptions
        // We also skip notifications for commands to avoid noise
        if (text.startsWith("/")) return@coroutineScope

        val wordsInMessage = text.split(Regex("\\s+"))
            .map { it.lowercase().filter { char -> char.isLetterOrDigit() } }
            .filter { it.length > 2 }
            .toSet()

        if (wordsInMessage.isEmpty()) return@coroutineScope

        // Find all unique keywords users are subscribed to
        // Note: For large systems, this should be optimized. For this bot, it's efficient enough.
        val allSubscribedKeywords = database.getAllSubscribedKeywords()
        
        for (keyword in allSubscribedKeywords) {
            // Get cached word forms for this keyword, or fetch them if missing
            var cachedForms = database.getWordForms(keyword)
            if (cachedForms == null) {
                println("[Cache] Missing word forms for '$keyword'. Fetching...")
                cachedForms = getWordForms(keyword)
                database.saveWordForms(keyword, cachedForms)
            }

            // Check if any word in the message matches any form of the keyword
            if (wordsInMessage.any { it in cachedForms }) {
                val subscribers = database.findSubscribersForKeywords(setOf(keyword))
                val userIds = subscribers[keyword] ?: continue

                userIds.forEach { subscriberId ->
                    // Skip if the subscriber is the one who sent the message
                    if (subscriberId == userId) return@forEach

                    launch {
                        try {
                            val searchResult = database.searchMessages(listOf(keyword), listOf(chatId), limit = 1).firstOrNull()
                            val messageLink = searchResult?.let { createMessageLink(it) } ?: ""
                            
                            val notificationText = "🔔 Word *'$keyword'* mentioned in *${chatTitle ?: "a group"}*:\n\n\"$text\"\n\n[Go to message]($messageLink)"
                            
                            bot.sendMessage(
                                chatId = ChatId.fromId(subscriberId),
                                text = notificationText,
                                parseMode = ParseMode.MARKDOWN,
                                disableWebPagePreview = true
                            ).fold(
                                { /* Success */ },
                                {
                                    val errorLog = "[Notification Error] Failed to send PM to $subscriberId: $it"
                                    println(errorLog)
                                    File("full_activity.log").appendText("$errorLog\n")
                                }
                            )
                        } catch (e: Exception) {
                            println("[Notification Fatal] Error notifying $subscriberId: ${e.message}")
                        }
                        delay(500)
                    }
                }
            }
        }
    } else {
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

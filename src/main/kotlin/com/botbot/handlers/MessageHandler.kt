package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.entities.Message
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun handleMessage(message: Message, database: MessageDatabase, monitoredChats: List<Long>) {
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

    // Only save the user who sent the message in this chat.
    user?.let { database.saveUser(it.id, it.firstName) }

    // If chat is monitored, save the message to the database
    if (chatId in monitoredChats) {
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
    } else {
        // This is an unmonitored chat.
        // Log its ID to the console and a file, but only once.
        if (chat.type == "supergroup" || chat.type == "group" || chat.type == "private" || chat.type == "channel") {
            val logFile = File("chat_ids.log")
            val logFileContent = if (logFile.exists()) logFile.readText() else ""

            val hasBeenLogged = logFileContent.lines().any { it.startsWith("$chatId|") }

            if (!hasBeenLogged) {
                val effectiveTitle = chatTitle ?: "Private Chat with ${user?.firstName ?: "user"}"
                val logTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                logFile.appendText("$chatId|$effectiveTitle|$logTimestamp\n")

                println("\n🎯 New unmonitored chat detected (logging to chat_ids.log):")
                println("   Type: ${chat.type}")
                println("   Title: $effectiveTitle")
                println("   ID: $chatId")
                println("   (Add this ID to your Config.kt to start saving messages)")
            }
        }
    }
}

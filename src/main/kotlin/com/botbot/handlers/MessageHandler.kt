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

    // Always save the user, regardless of the chat.
    message.from?.let { user ->
        database.saveUser(user.id, user.firstName)
    }

    // If chat is monitored, save the message
    if (chatId in monitoredChats) {
        val text = message.text ?: message.caption ?: ""
        if (text.isNotBlank()) {
            database.saveMessage(
                chatId = chatId,
                chatTitle = chatTitle,
                messageId = message.messageId,
                text = text,
                senderName = message.from?.firstName,
                senderId = message.from?.id,
                timestamp = message.date
            )
        }
    } else {
        // Otherwise, this is an unmonitored chat.
        // Log its ID to the console and a file, but only once.
        if (chat.type == "supergroup" || chat.type == "group" || chat.type == "private" || chat.type == "channel") {
            val logFile = File("chat_ids.log")
            val logFileContent = if (logFile.exists()) logFile.readText() else ""

            // Check if we've already logged this chat ID to avoid spamming the log.
            val hasBeenLogged = logFileContent.lines().any { it.startsWith("$chatId|") }

            if (!hasBeenLogged) {
                // For private chats, the title is the user's name.
                val effectiveTitle = chatTitle ?: "Private Chat with ${message.from?.firstName ?: "user"}"
                
                // Use modern java.time for accurate, timezone-aware timestamps.
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                // Log to file for persistence
                logFile.appendText("$chatId|$effectiveTitle|$timestamp\n")

                // Log to console to make it visible
                println("\n🎯 New unmonitored chat detected:")
                println("   Type: ${chat.type}")
                println("   Title: $effectiveTitle")
                println("   ID: $chatId")
                println("   (Add this ID to your Config.kt to start saving messages)")
            }
        }
    }
}

package com.botbot.handlers

import com.github.kotlintelegrambot.entities.Message
import java.io.File
import java.util.Date

fun handleLogging(message: Message) {
    // If the message is forwarded from a group
    if (message.forwardFromChat != null) {
        val forwardedChat = message.forwardFromChat!!
        println("Переслано из чата: ${forwardedChat.title} | ID: ${forwardedChat.id}")
    }

    // Log supergroup/group chats to a file
    val chat = message.chat
    if (chat.type == "supergroup" || chat.type == "group") {
        val logFile = File("chat_ids.log")
        val logFileContent = if (logFile.exists()) logFile.readText() else ""

        // Write to the hidden file
        logFile.appendText(
            "${chat.id}|${chat.title}|${Date()}\n"
        )

        // Show console output only for the first message
        if (!logFileContent.contains(chat.id.toString())) {
            println("\n🎯 ЧАТ ОБНАРУЖЕН (только в консоль):")
            println("ID: ${chat.id}")
            println("Название: ${chat.title}")
            println("\nФайл: chat_ids.log")
        }
    }
}

package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import java.io.File

suspend fun handleUnsubscribeAll(bot: Bot, message: Message, database: MessageDatabase) {
    val user = message.from ?: return

    database.removeAllSubscriptions(user.id)

    // Log the action
    val logMessage = "[Subscription] User ${user.firstName} (${user.id}) unsubscribed from ALL keywords"
    println(logMessage)
    File("full_activity.log").appendText("$logMessage\n")

    bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "✅ You have been unsubscribed from all keywords.")
}

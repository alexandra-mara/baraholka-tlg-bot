package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import java.io.File

suspend fun handleUnsubscribe(bot: Bot, message: Message, args: List<String>, database: MessageDatabase) {
    val keyword = args.firstOrNull()
    val user = message.from ?: return

    if (keyword == null) {
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Please provide a keyword to unsubscribe from. Usage: /unsubscribe [keyword]")
        return
    }

    database.removeSubscription(user.id, keyword)

    // Log the action
    val logMessage = "[Subscription] User ${user.firstName} (${user.id}) unsubscribed from '${keyword.lowercase()}'"
    println(logMessage)
    File("full_activity.log").appendText("$logMessage\n")

    bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "✅ You have been unsubscribed from updates for the keyword: '${keyword.lowercase()}'")
}

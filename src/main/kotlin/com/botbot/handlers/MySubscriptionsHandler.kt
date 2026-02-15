package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

suspend fun handleMySubscriptions(bot: Bot, message: Message, database: MessageDatabase) {
    val user = message.from ?: return
    val subscriptions = database.getSubscriptionsForUser(user.id)

    if (subscriptions.isEmpty()) {
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "You have no active subscriptions.")
    } else {
        val subsList = subscriptions.joinToString("\n") { "- `$it`" }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "*Your active subscriptions:*\n$subsList",
            parseMode = com.github.kotlintelegrambot.entities.ParseMode.MARKDOWN
        )
    }
}

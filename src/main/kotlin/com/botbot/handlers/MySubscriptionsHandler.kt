package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

suspend fun handleMySubscriptions(bot: Bot, message: Message, database: MessageDatabase) {
    val user = message.from ?: return
    val subscriptions = database.getSubscriptionsForUser(user.id)

    if (subscriptions.isEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "You have no active subscriptions."
        )
    } else {
        val keyboard = InlineKeyboardMarkup.create(
            subscriptions.map { keyword ->
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "❌ $keyword",
                        callbackData = "delete_sub:$keyword"
                    )
                )
            }
        )

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "*Your active subscriptions:*\nClick a button to remove a subscription:",
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = keyboard
        )
    }
}

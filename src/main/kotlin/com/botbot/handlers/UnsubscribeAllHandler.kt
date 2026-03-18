package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

suspend fun handleUnsubscribeAll(bot: Bot, message: Message, database: MessageDatabase) {
    val user = message.from ?: return
    val count = database.getSubscriptionsForUser(user.id).size

    if (count == 0) {
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "You have no active subscriptions to remove.")
        return
    }

    val keyboard = InlineKeyboardMarkup.create(
        listOf(
            listOf(
                InlineKeyboardButton.CallbackData("✅ Yes, remove all $count", "confirm_unsub_all"),
                InlineKeyboardButton.CallbackData("❌ Cancel", "cancel_unsub_all")
            )
        )
    )

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "⚠️ *Are you sure?*\nThis will permanently delete all *$count* of your keyword subscriptions.",
        parseMode = ParseMode.MARKDOWN,
        replyMarkup = keyboard
    )
}

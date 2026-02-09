package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode

fun handleChatId(bot: Bot, message: Message) {
    val chatId = message.chat.id
    bot.sendMessage(
        chatId = ChatId.fromId(chatId),
        text = "📊 This chat's ID: `$chatId`",
        parseMode = ParseMode.MARKDOWN
    )

    // Also save to log for yourself
    println("✅ Chat found: ${message.chat.title} → ID: $chatId")
}

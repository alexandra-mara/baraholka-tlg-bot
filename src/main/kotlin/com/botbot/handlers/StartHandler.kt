package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

fun handleStart(bot: Bot, message: Message) {
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "Hello, ${message.from?.firstName}!"
    )
}

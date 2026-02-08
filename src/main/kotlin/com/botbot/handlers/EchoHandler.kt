package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

fun handleEcho(bot: Bot, message: Message, args: List<String>) {
    val textToEcho = args.joinToString(" ")
    if (textToEcho.isNotEmpty()) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "🔊 $textToEcho"
        )
    } else {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Напиши: /echo [ваш текст]"
        )
    }
}

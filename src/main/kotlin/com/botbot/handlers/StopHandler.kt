package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

fun handleStop(bot: Bot, message: Message) {
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "Bot is stopping..."
    )
    bot.stopPolling()
}

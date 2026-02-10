package com.botbot.handlers

import com.botbot.services.getWordForms
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

suspend fun handleWordForms(bot: Bot, message: Message, args: List<String>) {
    val word = args.firstOrNull()
    if (word == null) {
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Please provide a word. Usage: /wordforms [word]")
        return
    }

    val wordForms = getWordForms(word)

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "Found ${wordForms.size} word forms for '$word':\n${wordForms.joinToString("\n")}"
    )
}

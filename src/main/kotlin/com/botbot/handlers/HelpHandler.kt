package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode

fun handleHelp(bot: Bot, message: Message) {
    val helpText = """
        🤖 *Доступные команды:*

        `/start` - Начать работу с ботом
        `/stop` - Остановить текущую сессию
        `/hi` - Поздороваться с ботом
        `/help` - Показать это сообщение
        `/echo [текст]` - Повторить ваш текст

        📝 Просто отправьте текст, и бот ответит!
    """.trimIndent()

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = helpText,
        parseMode = ParseMode.MARKDOWN
    )
}

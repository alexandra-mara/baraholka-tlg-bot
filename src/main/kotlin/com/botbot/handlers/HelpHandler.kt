package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode

fun handleHelp(bot: Bot, message: Message) {
    val helpText = """
        🤖 *Available commands:*

        `/start` - Start interacting with the bot
        `/stop` - Stop the current session
        `/help` - Show this message
        `/search [query]` - search for listings
        `/stats` - database statistics
        
         *Examples:*
                    /search tent
                    /search bicycle
                    /search apartment Limassol
         💡 Searches for the last 7 days

        📝 Just send a text, and the bot will answer!
    """.trimIndent()

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = helpText,
        parseMode = ParseMode.MARKDOWN
    )
}

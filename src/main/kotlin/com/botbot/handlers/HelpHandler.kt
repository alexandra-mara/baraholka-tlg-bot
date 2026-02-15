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
        `/search [query]` - search for listings in the current chat
        `/search_callback [query]` - search and get results in a private message
        `/stats` - database statistics
        `/show [count]` - show the last messages (default: 10)
        `/subscribe [keyword]` - get a notification when a keyword is mentioned
        `/unsubscribe [keyword]` - remove a subscription
        `/mysubs` - show your active subscriptions

        *Debug Commands:*
        `/users` - ⚠️ List all tracked users
        `/wordforms [word]` - Show all word forms for a given word
        
         *Examples:*
                    /search tent
                    /subscribe bicycle
    """.trimIndent()

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = helpText,
        parseMode = ParseMode.MARKDOWN
    )
}

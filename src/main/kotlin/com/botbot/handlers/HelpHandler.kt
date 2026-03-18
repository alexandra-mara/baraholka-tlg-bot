package com.botbot.handlers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

fun handleHelp(bot: Bot, message: Message, messageId: Long? = null) {
    val text = """
        🤖 *How can I help you today?*
        Choose a category below to see available commands.
    """.trimIndent()

    val keyboard = InlineKeyboardMarkup.create(
        listOf(
            listOf(
                InlineKeyboardButton.CallbackData("🔍 Search Help", "help_search"),
                InlineKeyboardButton.CallbackData("🔔 Subscriptions", "help_subs")
            ),
            listOf(
                InlineKeyboardButton.CallbackData("🛠️ Utilities", "help_utils"),
                InlineKeyboardButton.CallbackData("📜 Examples", "help_examples")
            )
        )
    )

    if (messageId != null) {
        bot.editMessageText(
            chatId = ChatId.fromId(message.chat.id),
            messageId = messageId,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = keyboard
        )
    } else {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = keyboard
        )
    }
}

fun showSearchHelp(bot: Bot, chatId: Long, messageId: Long) {
    val text = """
        🔍 *Search Commands:*
        
        `/search [query]` - Search in the current chat.
        `/search_callback [query]` - Search and get results in a PM.
        `/show [count]` - Show last messages (default: 10).
    """.trimIndent()
    
    val keyboard = InlineKeyboardMarkup.create(
        listOf(listOf(InlineKeyboardButton.CallbackData("⬅️ Back to Menu", "help_main")))
    )
    
    bot.editMessageText(ChatId.fromId(chatId), messageId, text = text, parseMode = ParseMode.MARKDOWN, replyMarkup = keyboard)
}

fun showSubsHelp(bot: Bot, chatId: Long, messageId: Long) {
    val text = """
        🔔 *Subscription Commands:*
        
        `/subscribe [keyword]` - Get notified of mentions.
        `/sub [keyword]` - Alias for subscribe.
        `/unsubscribe [keyword]` - Remove a subscription.
        `/unsubscribe_all` - Clear all your keywords.
        `/mysubs` - Manage your subscriptions with buttons.
    """.trimIndent()
    
    val keyboard = InlineKeyboardMarkup.create(
        listOf(listOf(InlineKeyboardButton.CallbackData("⬅️ Back to Menu", "help_main")))
    )
    
    bot.editMessageText(ChatId.fromId(chatId), messageId, text = text, parseMode = ParseMode.MARKDOWN, replyMarkup = keyboard)
}

fun showUtilsHelp(bot: Bot, chatId: Long, messageId: Long) {
    val text = """
        🛠️ *Utility Commands:*
        
        `/start` - Start the bot.
        `/stop` - Stop the bot.
        `/help` - Show this menu.
        `/chatid` - Get ID of current chat.
        `/stats` - DB statistics.
        `/users` - Debug: List users.
        `/wordforms` - Debug: Show word forms.
    """.trimIndent()
    
    val keyboard = InlineKeyboardMarkup.create(
        listOf(listOf(InlineKeyboardButton.CallbackData("⬅️ Back to Menu", "help_main")))
    )
    
    bot.editMessageText(ChatId.fromId(chatId), messageId, text = text, parseMode = ParseMode.MARKDOWN, replyMarkup = keyboard)
}

fun showExamplesHelp(bot: Bot, chatId: Long, messageId: Long) {
    val text = """
        📜 *Examples:*
        
        `/search tent`
        `/subscribe bicycle`
        `/show 5`
    """.trimIndent()
    
    val keyboard = InlineKeyboardMarkup.create(
        listOf(listOf(InlineKeyboardButton.CallbackData("⬅️ Back to Menu", "help_main")))
    )
    
    bot.editMessageText(ChatId.fromId(chatId), messageId, text = text, parseMode = ParseMode.MARKDOWN, replyMarkup = keyboard)
}

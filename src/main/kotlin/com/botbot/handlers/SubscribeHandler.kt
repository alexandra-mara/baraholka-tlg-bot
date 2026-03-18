package com.botbot.handlers

import com.botbot.db.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import java.io.File

suspend fun handleSubscribe(bot: Bot, message: Message, args: List<String>, database: MessageDatabase) {
    val keyword = args.firstOrNull()
    val user = message.from ?: return

    if (keyword == null) {
        val helpText = """
            Please provide a keyword to subscribe to. 
            Usage: `/subscribe [keyword]`
            
            To remove all your active subscriptions, use `/unsubscribe_all`.
        """.trimIndent()
        
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id), 
            text = helpText,
            parseMode = ParseMode.MARKDOWN
        )
        return
    }

    val added = database.addSubscription(user.id, keyword)
    val count = database.getSubscriptionsForUser(user.id).size

    if (added) {
        // Log the action
        val logMessage = "[Subscription] User ${user.firstName} (${user.id}) subscribed to '${keyword.lowercase()}'"
        println(logMessage)
        File("full_activity.log").appendText("$logMessage\n")

        val responseText = "✅ Successfully subscribed to '${keyword.lowercase()}'.\nYou have $count active subscriptions.\nRun /mysubs to see all of them."
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = responseText)
    } else {
        val responseText = "ℹ️ You are already subscribed to '${keyword.lowercase()}'.\nYou have $count active subscriptions.\nRun /mysubs to see all of them."
        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = responseText)
    }
}

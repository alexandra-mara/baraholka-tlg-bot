package com.botbot

import com.botbot.config.Config
import com.botbot.db.MessageDatabase
import com.botbot.handlers.*
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() {
    // Initialize the database
    val database = MessageDatabase()

    println("🤖 Bot starting...")
    println("📁 Database: messages_v5.db")
    println("📡 Monitored chats: ${Config.MONITORED_CHATS.size}")
    if (Config.MONITORED_CHATS.isEmpty()) {
        println("⚠️ MONITORED_CHATS is empty! Add chat_id for filtering.")
    }

    val bot = bot {
        this.token = Config.token

        dispatch {
            command("start") { CoroutineScope(Dispatchers.IO).launch { handleStart(bot, message) } }
            command("stop") { CoroutineScope(Dispatchers.IO).launch { handleStop(bot, message) } }
            command("help") { CoroutineScope(Dispatchers.IO).launch { handleHelp(bot, message) } }
            command("chatid") { CoroutineScope(Dispatchers.IO).launch { handleChatId(bot, message) } }
            command("search") { CoroutineScope(Dispatchers.IO).launch { handleSearch(bot, message, args, database, Config.MONITORED_CHATS) } }
            command("search_callback") { CoroutineScope(Dispatchers.IO).launch { handleSearchCallback(bot, message, args, database, Config.MONITORED_CHATS) } }
            command("stats") { CoroutineScope(Dispatchers.IO).launch { handleStats(bot, message, database, Config.MONITORED_CHATS) } }
            command("show") { CoroutineScope(Dispatchers.IO).launch { handleShow(bot, message, args, database) } }
            command("users") { CoroutineScope(Dispatchers.IO).launch { handleUsers(bot, message, database) } }
            command("wordforms") { CoroutineScope(Dispatchers.IO).launch { handleWordForms(bot, message, args) } }
            command("subscribe") { CoroutineScope(Dispatchers.IO).launch { handleSubscribe(bot, message, args, database) } }
            command("sub") { CoroutineScope(Dispatchers.IO).launch { handleSubscribe(bot, message, args, database) } }
            command("unsubscribe") { CoroutineScope(Dispatchers.IO).launch { handleUnsubscribe(bot, message, args, database) } }
            command("unsubscribe_all") { CoroutineScope(Dispatchers.IO).launch { handleUnsubscribeAll(bot, message, database) } }
            command("mysubs") { CoroutineScope(Dispatchers.IO).launch { handleMySubscriptions(bot, message, database) } }
            
            callbackQuery("delete_sub:") {
                val keyword = callbackQuery.data.removePrefix("delete_sub:")
                val userId = callbackQuery.from.id
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery

                database.removeSubscription(userId, keyword)
                bot.answerCallbackQuery(callbackQuery.id, text = "Removed: $keyword")
                
                val updatedSubs = database.getSubscriptionsForUser(userId)
                if (updatedSubs.isEmpty()) {
                    bot.editMessageText(ChatId.fromId(chatId), callbackQuery.message?.messageId, text = "You have no active subscriptions.")
                } else {
                    val keyboard = InlineKeyboardMarkup.create(updatedSubs.map { kw -> listOf(InlineKeyboardButton.CallbackData("❌ $kw", "delete_sub:$kw")) })
                    bot.editMessageText(ChatId.fromId(chatId), callbackQuery.message?.messageId, text = "*Your active subscriptions:*\nClick a button to remove a subscription:", parseMode = ParseMode.MARKDOWN, replyMarkup = keyboard)
                }
            }

            callbackQuery("confirm_unsub_all") {
                val userId = callbackQuery.from.id
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                database.removeAllSubscriptions(userId)
                bot.answerCallbackQuery(callbackQuery.id, text = "All subscriptions removed")
                bot.editMessageText(ChatId.fromId(chatId), callbackQuery.message?.messageId, text = "✅ All subscriptions have been removed.")
            }

            callbackQuery("cancel_unsub_all") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.answerCallbackQuery(callbackQuery.id, text = "Cancelled")
                bot.editMessageText(ChatId.fromId(chatId), callbackQuery.message?.messageId, text = "Action cancelled. Your subscriptions are safe.")
            }

            callbackQuery("subscribe_query:") {
                val keyword = callbackQuery.data.removePrefix("subscribe_query:")
                val user = callbackQuery.from
                val userId = user.id
                val added = database.addSubscription(userId, keyword)
                
                val count = database.getSubscriptionsForUser(userId).size
                val statusEmoji = if (added) "✅" else "ℹ️"
                val statusText = if (added) "Successfully subscribed to" else "You are already subscribed to"
                
                val messageText = "$statusEmoji $statusText '$keyword'.\nYou have $count active subscriptions.\nRun /mysubs to see all of them."
                
                bot.answerCallbackQuery(callbackQuery.id)
                
                bot.sendMessage(
                    chatId = ChatId.fromId(userId),
                    text = messageText
                ).fold({}, {
                    val chatId = callbackQuery.message?.chat?.id
                    if (chatId != null) {
                        bot.sendMessage(ChatId.fromId(chatId), text = "@${user.username ?: user.firstName}, $messageText")
                    }
                })
            }

            callbackQuery("help_") {
                val action = callbackQuery.data
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                val messageId = callbackQuery.message?.messageId ?: return@callbackQuery

                when (action) {
                    "help_main" -> handleHelp(bot, callbackQuery.message!!, messageId)
                    "help_search" -> showSearchHelp(bot, chatId, messageId)
                    "help_subs" -> showSubsHelp(bot, chatId, messageId)
                    "help_utils" -> showUtilsHelp(bot, chatId, messageId)
                    "help_examples" -> showExamplesHelp(bot, chatId, messageId)
                }
                bot.answerCallbackQuery(callbackQuery.id)
            }

            message { handleMessage(bot, message, database, Config.MONITORED_CHATS) }
        }
    }

    bot.startPolling()
    println("✅ Bot started!")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("\n👋 Stopping bot...")
        database.close()
        println("✅ Database saved")
    })
}

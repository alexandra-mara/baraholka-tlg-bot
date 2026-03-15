package com.botbot

import com.botbot.config.Config
import com.botbot.db.MessageDatabase
import com.botbot.handlers.*
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            message { handleMessage(bot, message, database, Config.MONITORED_CHATS) }
        }
    }

    bot.startPolling()
    println("✅ Bot started!")

    // Graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\n👋 Stopping bot...")
        database.close()
        println("✅ Database saved")
    })
}

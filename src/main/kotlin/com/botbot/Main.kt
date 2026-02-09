package com.botbot

import com.botbot.config.Config
import com.botbot.handlers.handleChatId
import com.botbot.handlers.handleEcho
import com.botbot.handlers.handleHelp
import com.botbot.handlers.handleHi
import com.botbot.handlers.handleMessage
import com.botbot.handlers.handleSearch
import com.botbot.handlers.handleStart
import com.botbot.handlers.handleStats
import com.botbot.handlers.handleStop
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    // Initialize the database
    val database = MessageDatabase()

    println("🤖 Bot starting...")
    println("📁 Database: messages_v2.db")
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
            command("hi") { CoroutineScope(Dispatchers.IO).launch { handleHi(bot, message) } }
            command("echo") { CoroutineScope(Dispatchers.IO).launch { handleEcho(bot, message, args) } }
            command("chatid") { CoroutineScope(Dispatchers.IO).launch { handleChatId(bot, message) } }
            command("search") { CoroutineScope(Dispatchers.IO).launch { handleSearch(bot, message, args, database, Config.MONITORED_CHATS) } }
            command("stats") { CoroutineScope(Dispatchers.IO).launch { handleStats(bot, message, database, Config.MONITORED_CHATS) } }
            text("привет") { CoroutineScope(Dispatchers.IO).launch { handleHi(bot, message) } }
            message { CoroutineScope(Dispatchers.IO).launch { handleMessage(message, database, Config.MONITORED_CHATS) } }
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

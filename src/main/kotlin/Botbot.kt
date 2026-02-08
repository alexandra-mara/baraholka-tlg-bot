package com.botbot

import com.botbot.config.Config
import com.botbot.handlers.handleEcho
import com.botbot.handlers.handleHelp
import com.botbot.handlers.handleHi
import com.botbot.handlers.handleStart
import com.botbot.handlers.handleStop
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    println("🤖 Bot starting...")

    val bot = bot {
        this.token = Config.token

        dispatch {
            command("start") { CoroutineScope(Dispatchers.IO).launch { handleStart(bot, message) } }
            command("stop") { CoroutineScope(Dispatchers.IO).launch { handleStop(bot, message) } }
            command("help") { CoroutineScope(Dispatchers.IO).launch { handleHelp(bot, message) } }
            command("hi") { CoroutineScope(Dispatchers.IO).launch { handleHi(bot, message) } }
            command("echo") { CoroutineScope(Dispatchers.IO).launch { handleEcho(bot, message, args) } }
            text("привет") { CoroutineScope(Dispatchers.IO).launch { handleHi(bot, message) } }
        }
    }

    bot.startPolling()
    println("✅ Bot started!")
}

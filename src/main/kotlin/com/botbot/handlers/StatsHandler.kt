package com.botbot.handlers

import com.botbot.MessageDatabase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import java.time.format.DateTimeFormatter

fun handleStats(bot: Bot, message: Message, database: MessageDatabase, monitoredChats: List<Long>) {
    val stats = database.getStats()
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = """
        📊 *Listing database statistics:*
        
        💬 Messages: *${stats.totalMessages}*
        👥 Users: *${stats.totalUsers}*
        📁 Chats: *${stats.totalChats}*
        
        📅 Oldest: ${
            stats.oldestMessage?.format(dateFormatter) ?: "no data"
        }
        📅 Newest: ${
            stats.newestMessage?.format(dateFormatter) ?: "no data"
        }
        
        🔍 Chats to search: ${monitoredChats.size}
        ${monitoredChats.joinToString("\n") { "   • $it" }}
        
        *Use:* /search [query]
        """.trimIndent(),
        parseMode = ParseMode.MARKDOWN
    )
}

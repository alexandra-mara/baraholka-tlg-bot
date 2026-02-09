package com.botbot.config

import java.io.File

object Config {
    val token: String by lazy {
        readToken()
    }

    val MONITORED_CHATS: List<Long> = listOf(
        // Insert your chat_id after receiving it
        // -1001234567890L, // CYPRUS 🇨🇾 FLEA MARKET
        // -1009876543210L  // CypRusSale
    )

    private fun readToken(): String {
        // 1. Try from environment variables
        val fromEnv = System.getenv("TELEGRAM_BOT_TOKEN")
        if (!fromEnv.isNullOrEmpty()) {
            return fromEnv
        }

        // 2. Try from .env file
        val fromFile = readFromEnvFile(".env")
        if (!fromFile.isNullOrEmpty()) {
            return fromFile
        }

        // 3. If still not found, throw an error
        error("""
            ⚠️  Token not found!

            Create '.env' file with:
            TELEGRAM_BOT_TOKEN=your_token_here

            Or set environment variable:
            export TELEGRAM_BOT_TOKEN=your_token_here
        """.trimIndent())
    }

    private fun readFromEnvFile(filename: String): String? {
        return try {
            File(filename).readLines()
                .find { it.startsWith("TELEGRAM_BOT_TOKEN=") }
                ?.substringAfter("=")
                ?.trim()
        } catch (e: Exception) {
            null
        }
    }
}

package com.botbot.utils

import com.botbot.db.model.SearchResult

fun createMessageLink(result: SearchResult): String {
    return if (result.chatUsername != null) {
        // Public chat or channel
        "https://t.me/${result.chatUsername}/${result.messageId}"
    } else {
        // Private group or supergroup
        // The strange number is because of how Telegram handles private chat links
        val privateChatId = result.chatId.toString().removePrefix("-100")
        "https://t.me/c/${privateChatId}/${result.messageId}"
    }
}

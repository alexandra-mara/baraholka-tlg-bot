package com.botbot

import java.sql.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant

class MessageDatabase {
    private var connection: Connection

    init {
        // Создаём или подключаемся к базе
        connection = DriverManager.getConnection("jdbc:sqlite:messages_v2.db")
        createTables()
        println("✅ База данных подключена: messages_v2.db")
    }

    private fun createTables() {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id BIGINT NOT NULL,
                chat_title TEXT,
                message_id BIGINT NOT NULL,
                message_text TEXT NOT NULL,
                sender_name TEXT,
                sender_id BIGINT,
                timestamp BIGINT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(chat_id, message_id)
            )
        """

        val createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_search 
            ON messages(message_text, timestamp)
        """

        connection.createStatement().use { stmt ->
            stmt.execute(createTableSQL)
            stmt.execute(createIndexSQL)
        }
    }

    fun saveMessage(
        chatId: Long,
        chatTitle: String?,
        messageId: Long,
        text: String,
        senderName: String?,
        senderId: Long?,
        timestamp: Long // Unix timestamp из Telegram
    ) {
        val sql = """
            INSERT OR IGNORE INTO messages 
            (chat_id, chat_title, message_id, message_text, sender_name, sender_id, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, chatId)
                pstmt.setString(2, chatTitle)
                pstmt.setLong(3, messageId)
                pstmt.setString(4, text)
                pstmt.setString(5, senderName)
                senderId?.let { pstmt.setLong(6, it) } ?: pstmt.setNull(6, Types.BIGINT)
                pstmt.setLong(7, timestamp)
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Ошибка сохранения сообщения: ${e.message}")
        }
    }

    fun searchMessages(
    query: List<String>,
    chatIds: List<Long>? = null,
    daysBack: Int = 7,
    limit: Int = 10
): List<SearchResult> {
    if (query.isEmpty()) return emptyList()

    val timeBoundary = Instant.now().minus(daysBack.toLong(), java.time.temporal.ChronoUnit.DAYS).epochSecond
    val wordFormsWhere = query.joinToString(separator = " OR ") { "message_text LIKE ?" }
    val chatIdsWhere = if (chatIds != null && chatIds.isNotEmpty()) {
        "AND chat_id IN (${chatIds.map { "?" }.joinToString()})"
    } else ""

    val sql = """
        SELECT * FROM (
            SELECT
                chat_id,
                chat_title,
                message_id,
                message_text,
                sender_name,
                timestamp,
                ROW_NUMBER() OVER(PARTITION BY message_text ORDER BY timestamp DESC) as rn
            FROM messages
            WHERE ($wordFormsWhere)
            AND timestamp >= ?
            $chatIdsWhere
        )
        WHERE rn = 1
        ORDER BY timestamp DESC
        LIMIT ?
        """

    return connection.prepareStatement(sql).use { pstmt ->
        var index = 1
        query.forEach { pstmt.setString(index++, "%$it%") }
        pstmt.setLong(index++, timeBoundary)
        chatIds?.forEach { pstmt.setLong(index++, it) }
        pstmt.setInt(index, limit)

        pstmt.executeQuery().use { rs ->
            val results = mutableListOf<SearchResult>()
            while (rs.next()) {
                val timestamp = rs.getLong("timestamp")
                val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
                results.add(SearchResult(
                    chatId = rs.getLong("chat_id"),
                    chatTitle = rs.getString("chat_title"),
                    messageId = rs.getLong("message_id"),
                    text = rs.getString("message_text"),
                    senderName = rs.getString("sender_name"),
                    timestamp = dateTime
                ))
            }
            results
        }
    }
}

    fun getStats(): DatabaseStats {
        val sql = """
            SELECT 
                COUNT(*) as total_messages,
                COUNT(DISTINCT chat_id) as total_chats,
                MIN(timestamp) as oldest_message,
                MAX(timestamp) as newest_message
            FROM messages
        """

        return connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) {
                    val oldestTimestamp = rs.getLong("oldest_message")
                    val oldestWasNull = rs.wasNull()
                    val newestTimestamp = rs.getLong("newest_message")
                    val newestWasNull = rs.wasNull()

                    val oldestDateTime = if (oldestWasNull) null else LocalDateTime.ofInstant(Instant.ofEpochSecond(oldestTimestamp), ZoneId.systemDefault())
                    val newestDateTime = if (newestWasNull) null else LocalDateTime.ofInstant(Instant.ofEpochSecond(newestTimestamp), ZoneId.systemDefault())

                    DatabaseStats(
                        totalMessages = rs.getInt("total_messages"),
                        totalChats = rs.getInt("total_chats"),
                        oldestMessage = oldestDateTime,
                        newestMessage = newestDateTime
                    )
                } else {
                    DatabaseStats()
                }
            }
        }
    }

    fun close() {
        connection.close()
    }
}

data class SearchResult(
    val chatId: Long,
    val chatTitle: String,
    val messageId: Long,
    val text: String,
    val senderName: String?,
    val timestamp: LocalDateTime
)

data class DatabaseStats(
    val totalMessages: Int = 0,
    val totalChats: Int = 0,
    val oldestMessage: LocalDateTime? = null,
    val newestMessage: LocalDateTime? = null
)
package com.botbot.db

import com.botbot.db.model.DatabaseStats
import com.botbot.db.model.SearchResult
import com.botbot.db.model.Subscription
import com.botbot.db.model.User
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MessageDatabase {
    private var connection: Connection

    init {
        // Create or connect to the database
        connection = DriverManager.getConnection("jdbc:sqlite:messages_v5.db")
        createTables()
        println("✅ Database connected: messages_v5.db")
    }

    private fun createTables() {
        val createMessagesTableSQL = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id BIGINT NOT NULL,
                chat_title TEXT,
                chat_username TEXT, 
                message_id BIGINT NOT NULL,
                message_text TEXT NOT NULL,
                sender_name TEXT,
                sender_id BIGINT,
                forwarded_from_id BIGINT,
                forwarded_from_name TEXT,
                timestamp BIGINT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(chat_id, message_id)
            )
        """

        val createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                user_id BIGINT PRIMARY KEY,
                user_name TEXT,
                first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """

        val createSubscriptionsTableSQL = """
            CREATE TABLE IF NOT EXISTS subscriptions (
                subscription_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id BIGINT NOT NULL,
                keyword TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, keyword)
            )
        """

        val createWordFormsTableSQL = """
            CREATE TABLE IF NOT EXISTS word_forms (
                base_word TEXT PRIMARY KEY,
                forms TEXT NOT NULL,
                last_updated DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """

        val createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_search 
            ON messages(message_text, timestamp)
        """

        val createSubscriptionIndexSQL = "CREATE INDEX IF NOT EXISTS idx_subscription_keyword ON subscriptions(keyword)"

        connection.createStatement().use { stmt ->
            stmt.execute(createMessagesTableSQL)
            stmt.execute(createUsersTableSQL)
            stmt.execute(createSubscriptionsTableSQL)
            stmt.execute(createWordFormsTableSQL)
            stmt.execute(createIndexSQL)
            stmt.execute(createSubscriptionIndexSQL)
        }
    }

    fun getAllSubscribedKeywords(): Set<String> {
        val sql = "SELECT DISTINCT keyword FROM subscriptions"
        val keywords = mutableSetOf<String>()
        try {
            connection.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        keywords.add(rs.getString("keyword"))
                    }
                }
            }
        } catch (e: SQLException) {
            println("⚠️ Error getting all subscribed keywords: ${e.message}")
        }
        return keywords
    }

    fun getWordForms(baseWord: String): List<String>? {
        val sql = "SELECT forms FROM word_forms WHERE base_word = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, baseWord.lowercase())
                pstmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return rs.getString("forms").split(",")
                    }
                }
            }
        } catch (e: SQLException) {
            println("⚠️ Error getting word forms: ${e.message}")
        }
        return null
    }

    fun saveWordForms(baseWord: String, forms: List<String>) {
        val sql = "INSERT OR REPLACE INTO word_forms (base_word, forms, last_updated) VALUES (?, ?, CURRENT_TIMESTAMP)"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, baseWord.lowercase())
                pstmt.setString(2, forms.joinToString(","))
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Error saving word forms: ${e.message}")
        }
    }

    fun findSubscribersForKeywords(keywords: Set<String>): Map<String, List<Long>> {
        if (keywords.isEmpty()) return emptyMap()

        val placeholders = keywords.joinToString(",") { "?" }
        val sql = "SELECT keyword, user_id FROM subscriptions WHERE keyword IN ($placeholders)"
        val subscribers = mutableMapOf<String, MutableList<Long>>()

        try {
            connection.prepareStatement(sql).use { pstmt ->
                keywords.forEachIndexed { index, keyword ->
                    pstmt.setString(index + 1, keyword)
                }
                pstmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val keyword = rs.getString("keyword")
                        val userId = rs.getLong("user_id")
                        subscribers.getOrPut(keyword) { mutableListOf() }.add(userId)
                    }
                }
            }
        } catch (e: SQLException) {
            println("⚠️ Error finding subscribers: ${e.message}")
        }
        return subscribers
    }

    /**
     * Adds a subscription for a user.
     * Returns true if a new subscription was added, false if it already existed.
     */
    fun addSubscription(userId: Long, keyword: String): Boolean {
        val sql = "INSERT OR IGNORE INTO subscriptions (user_id, keyword) VALUES (?, ?)"
        return try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, userId)
                pstmt.setString(2, keyword.lowercase())
                pstmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            println("⚠️ Error adding subscription: ${e.message}")
            false
        }
    }

    fun removeSubscription(userId: Long, keyword: String) {
        val sql = "DELETE FROM subscriptions WHERE user_id = ? AND keyword = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, userId)
                pstmt.setString(2, keyword.lowercase())
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Error removing subscription: ${e.message}")
        }
    }

    fun removeAllSubscriptions(userId: Long) {
        val sql = "DELETE FROM subscriptions WHERE user_id = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, userId)
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Error removing all subscriptions: ${e.message}")
        }
    }

    fun getSubscriptionsForUser(userId: Long): List<String> {
        val sql = "SELECT keyword FROM subscriptions WHERE user_id = ? ORDER BY keyword"
        val keywords = mutableListOf<String>()
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, userId)
                pstmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        keywords.add(rs.getString("keyword"))
                    }
                }
            }
        } catch (e: SQLException) {
            println("⚠️ Error getting subscriptions: ${e.message}")
        }
        return keywords
    }

    fun saveMessage(
        chatId: Long,
        chatTitle: String?,
        chatUsername: String?,
        messageId: Long,
        text: String,
        senderName: String?,
        senderId: Long?,
        forwardedFromId: Long?,
        forwardedFromName: String?,
        timestamp: Long // Unix timestamp from Telegram
    ) {
        val sql = """
            INSERT OR REPLACE INTO messages 
            (chat_id, chat_title, chat_username, message_id, message_text, sender_name, sender_id, forwarded_from_id, forwarded_from_name, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, chatId)
                pstmt.setString(2, chatTitle)
                pstmt.setString(3, chatUsername)
                pstmt.setLong(4, messageId)
                pstmt.setString(5, text)
                pstmt.setString(6, senderName)
                senderId?.let { pstmt.setLong(7, it) } ?: pstmt.setNull(7, Types.BIGINT)
                forwardedFromId?.let { pstmt.setLong(8, it) } ?: pstmt.setNull(8, Types.BIGINT)
                pstmt.setString(9, forwardedFromName)
                pstmt.setLong(10, timestamp)
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Error saving message: ${e.message}")
        }
    }

    fun saveUser(userId: Long, userName: String?) {
        val sql = "INSERT OR IGNORE INTO users (user_id, user_name) VALUES (?, ?)"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setLong(1, userId)
                pstmt.setString(2, userName ?: "")
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println("⚠️ Error saving user: ${e.message}")
        }
    }

    fun getAllUsers(): List<User> {
        val sql = "SELECT user_id, user_name FROM users ORDER BY first_seen_at DESC"
        return connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val users = mutableListOf<User>()
                while (rs.next()) {
                    users.add(User(
                        id = rs.getLong("user_id"),
                        name = rs.getString("user_name")
                    ))
                }
                users
            }
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
        "AND m.chat_id IN (${chatIds.map { "?" }.joinToString()})"
    } else ""

    val sql = """
        SELECT * FROM (
            SELECT
                chat_id,
                chat_title,
                chat_username,
                message_id,
                message_text,
                sender_name,
                timestamp,
                ROW_NUMBER() OVER(PARTITION BY message_text ORDER BY timestamp DESC) as rn
            FROM messages m
            WHERE ($wordFormsWhere)
            AND message_text NOT LIKE '/%'
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
                    chatUsername = rs.getString("chat_username"),
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

    fun showLastMessages(limit: Int = 10): List<SearchResult> {
        val sql = """
            SELECT
                chat_id,
                chat_title,
                chat_username,
                message_id,
                message_text,
                sender_name,
                timestamp
            FROM messages
            ORDER BY timestamp DESC
            LIMIT ?
        """

        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, limit)

            pstmt.executeQuery().use { rs ->
                val results = mutableListOf<SearchResult>()
                while (rs.next()) {
                    val timestamp = rs.getLong("timestamp")
                    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
                    results.add(SearchResult(
                        chatId = rs.getLong("chat_id"),
                        chatTitle = rs.getString("chat_title"),
                        chatUsername = rs.getString("chat_username"),
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
        val messageStatsSql = """
            SELECT 
                COUNT(*) as total_messages,
                COUNT(DISTINCT chat_id) as total_chats,
                MIN(timestamp) as oldest_message,
                MAX(timestamp) as newest_message
            FROM messages
        """
        val userStatsSql = "SELECT COUNT(*) as total_users FROM users"

        var stats = DatabaseStats()

        connection.createStatement().use { stmt ->
            stmt.executeQuery(messageStatsSql).use { rs ->
                if (rs.next()) {
                    val oldestTimestamp = rs.getLong("oldest_message")
                    val oldestWasNull = rs.wasNull()
                    val newestTimestamp = rs.getLong("newest_message")
                    val newestWasNull = rs.wasNull()

                    val oldestDateTime = if (oldestWasNull) null else LocalDateTime.ofInstant(Instant.ofEpochSecond(oldestTimestamp), ZoneId.systemDefault())
                    val newestDateTime = if (newestWasNull) null else LocalDateTime.ofInstant(Instant.ofEpochSecond(newestTimestamp), ZoneId.systemDefault())

                    stats = stats.copy(
                        totalMessages = rs.getInt("total_messages"),
                        totalChats = rs.getInt("total_chats"),
                        oldestMessage = oldestDateTime,
                        newestMessage = newestDateTime
                    )
                }
            }
            stmt.executeQuery(userStatsSql).use { rs ->
                if (rs.next()) {
                    stats = stats.copy(totalUsers = rs.getInt("total_users"))
                }
            }
        }
        return stats
    }

    fun close() {
        connection.close()
    }
}

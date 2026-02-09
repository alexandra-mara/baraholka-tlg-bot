# Telegram Message Saver & Search Bot

A Kotlin-based Telegram bot designed to automatically save messages from specific group chats, supergroups, and channels into a local SQLite database. It provides a powerful search functionality to find messages across all monitored chats.

## Features

- **Automatic Message Archiving**: Saves messages from configured Telegram chats into a local SQLite database (`messages_v2.db`).
- **Advanced Search**: The `/search` command supports finding all word forms of a query (e.g., searching for 'run' will also find 'ran' and 'running').
- **Unique Results**: The search functionality returns only the latest unique message based on its content, avoiding duplicates.
- **Database Statistics**: The `/stats` command provides an overview of the database, including total messages, total chats, and the date range of stored messages.
- **Chat ID Discovery**: Includes a `/chatid` command and a silent detection feature to easily find the IDs of new or unmonitored chats.
- **Secure Configuration**: Bot token is managed securely via an environment variable or a local `.env` file.
- **Modern Tech Stack**: Built with Kotlin, Coroutines, and Gradle.

## Getting Started

### Prerequisites

- JDK 21 or higher.
- A Telegram Bot Token (get one from [@BotFather](https://t.me/BotFather)).

### Setup & Configuration

1.  **Clone the repository:**
    ```bash
    git clone <repository_url>
    cd tlg-bot
    ```

2.  **Configure the Bot Token:**
    Create a file named `.env` in the root of the project and add your Telegram bot token:
    ```
    TELEGRAM_BOT_TOKEN=your_token_here
    ```
    Alternatively, you can set `TELEGRAM_BOT_TOKEN` as an environment variable.

3.  **Configure Monitored Chats:**
    Open `src/main/kotlin/com/botbot/config/Config.kt` and add the chat IDs you want the bot to monitor to the `MONITORED_CHATS` list. For example:
    ```kotlin
    val MONITORED_CHATS: List<Long> = listOf(
        -1001234567890L, // Example Chat 1
        -1009876543210L  // Example Chat 2
    )
    ```
    *To get a chat ID, add the bot to the chat and use the `/chatid` command or check the console output.* 

### Running the Bot

Use the included Gradle wrapper to build and run the application:

```bash
./gradlew run
```

The bot will start polling for updates.

## Usage

- `/start`: Starts the bot.
- `/stop`: Stops the bot.
- `/help`: Displays a list of available commands and examples.
- `/search <query>`: Searches the database for messages. The search is case-insensitive and matches all word forms.
- `/stats`: Shows statistics about the messages stored in the database.
- `/chatid`: Responds with the unique ID of the current chat.

### Silent Chat ID Detection

If the bot is a member of a chat that is **not** in your `MONITORED_CHATS` list, it will automatically print the chat's ID to the console the first time a message is sent there. This makes it easy to find the IDs of new chats you wish to monitor.

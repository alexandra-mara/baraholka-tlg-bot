# Telegram Message Saver & Search Bot

A Kotlin-based Telegram bot designed to automatically save messages from specific group chats and notify users based on keyword subscriptions.

## Features

- **Automatic Message Archiving**: Saves messages from configured Telegram chats into a local SQLite database.
- **Keyword Subscriptions**: Users can `/subscribe` to keywords and receive instant private notifications when those words appear in monitored chats.
- **Advanced Search**: The `/search` command supports finding all word forms of a query from multiple online sources.
- **Callback Search**: The `/search_callback` command performs a search and delivers the results to you in a private message.
- **Direct Message Links**: All notifications and search results include a direct link to the original message.
- **User Tracking**: Counts the number of unique users who have interacted with the bot.
- **Database Statistics**: The `/stats` command provides an overview of the database, including total messages, users, and chats.

## Getting Started

### Prerequisites

- JDK 21 or higher.
- A Telegram Bot Token (get one from [@BotFather](https://t.me/BotFather)).

### Setup & Configuration

1.  **Clone the repository.**

2.  **Configure the Bot Token:**
    - Run `./start.sh`. It will create a `.env` file if one doesn't exist.
    - Add your Telegram bot token to the `.env` file.

3.  **Important: Disable Group Privacy Mode**
    - For the bot to receive all messages in a group, you **must** disable its privacy mode via **@BotFather** using the `/setprivacy` command.
    - Alternatively, making the bot an administrator of the group also works.

4.  **Configure Monitored Chats:**
    - Open `src/main/kotlin/com/botbot/config/Config.kt` and add the chat IDs you want the bot to monitor.

### Running the Bot

```bash
chmod +x start.sh
./start.sh
```

## Usage

### Subscription Commands
- `/subscribe <keyword>`: Get a notification when a keyword is mentioned.
- `/sub <keyword>`: Alias for `/subscribe`.
- `/unsubscribe <keyword>`: Remove a subscription.
- `/unsubscribe_all`: Remove all your active subscriptions.
- `/mysubs`: Show your active subscriptions.

### Search & History Commands
- `/search <query>`: Searches the database for messages in the current chat.
- `/search_callback <query>`: Performs a search and delivers the results to you in a private message.
- `/show [count]`: Shows the last N messages from the database (default: 10).

### Utility Commands
- `/start` & `/stop`: Starts and stops the bot.
- `/help`: Displays the command list.
- `/chatid`: Responds with the unique ID of the current chat.
- `/stats`: Shows database statistics.

### Debug Commands
- `/users`: ⚠️ List all tracked users (Debug only).
- `/wordforms <word>`: Show all word forms for a given word.

### Important Note on Private Messages

For commands like `/search_callback` or for receiving notifications, you must have first initiated a private chat with the bot (e.g., by sending `/start`).

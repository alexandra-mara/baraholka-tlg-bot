# Telegram Message Saver & Search Bot

A Kotlin-based Telegram bot designed to automatically save messages from specific group chats and notify users based on keyword subscriptions.

## Features

- **Automatic Message Archiving**: Saves messages from configured Telegram chats into a local SQLite database.
- **Keyword Subscriptions**: Users can `/subscribe` to keywords and receive instant private notifications.
- **Optimized Monitoring**: High-performance keyword matching that doesn't rely on external APIs for every message.
- **Advanced Search**: The `/search` command supports finding all word forms of a query from multiple online sources.
- **Callback Search**: Performs searches and delivers results in private messages.
- **Resilient Networking**: Automatic retries with exponential backoff and timeouts for all external API requests.
- **Direct Message Links**: Includes direct links to the original messages in notifications and search results.
- **Discoverability**: Automatically logs unknown chat IDs to `chat_ids.log` for easy configuration.

## Getting Started

### Prerequisites

- JDK 21 or higher.
- A Telegram Bot Token (get one from [@BotFather](https://t.me/BotFather)).

### Setup & Configuration

1.  **Clone the repository.**
2.  **Configure the Bot Token**: Run `./start.sh` to generate a `.env` file and add your token.
3.  **Disable Group Privacy Mode**: Use `/setprivacy` via @BotFather or make the bot an admin so it can see all messages.
4.  **Configure Monitored Chats**: 
    - Add chat IDs to `src/main/kotlin/com/botbot/config/Config.kt`.
    - You can find the IDs of chats the bot is in by checking `chat_ids.log`.

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
- `/search_callback <query>`: Performs a search and delivers results in a private message.
- `/show [count]`: Shows the last N messages (default: 10).

### Utility Commands
- `/start` & `/stop`: Starts and stops the bot session.
- `/help`: Displays the command list.
- `/chatid`: Returns the unique ID of the current chat.
- `/stats`: Shows database statistics (messages, users, chats).

### Debug Commands
- `/users`: ⚠️ List all tracked users (Debug only).
- `/wordforms <word>`: Show all word forms for a given word.

## Important Notes

- **Private Messages**: You must initiate a private chat with the bot (`/start`) to receive notifications or search results.
- **Self-Notifications**: The bot will **not** notify you about your own messages, even if they contain your subscribed keywords.
- **Keyword Matching**: Notifications use exact word matching for speed and reliability. Use `/search` for broad matching including word forms.

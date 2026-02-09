plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Telegram bot
    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // SQLite for database
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Kotlinx DateTime for working with dates
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // If you want full-text search
    implementation("com.github.ajalt.clikt:clikt:4.2.0") // for command line

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

application {
    mainClass.set("com.botbot.MainKt")
}

kotlin {
    jvmToolchain(21)
}

// Disable distribution archive tasks
tasks.named<Zip>("distZip") {
    enabled = false
}

tasks.named<Tar>("distTar") {
    enabled = false
}

// handle duplicates for all Copy tasks
tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

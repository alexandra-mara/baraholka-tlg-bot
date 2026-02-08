plugins {
    kotlin("jvm") version "2.3.10"
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

application {
    mainClass.set("com.botbot.BotbotKt")
}

kotlin {
    jvmToolchain(21)
}

// Отключаем задачи создания архивов дистрибуции
tasks.named<Zip>("distZip") {
    enabled = false
}

tasks.named<Tar>("distTar") {
    enabled = false
}

// ИЛИ добавьте обработку дубликатов для всех Copy задач
tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

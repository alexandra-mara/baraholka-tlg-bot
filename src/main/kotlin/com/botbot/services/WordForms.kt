package com.botbot.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class ApiResponse(val status: Int, val items: List<String>)

suspend fun getWordForms(baseWord: String): List<String> {
    val apiUrl = "https://htmlweb.ru/json/service/inflect?inflect=$baseWord"
    return try {
        val jsonText = URL(apiUrl).readText()
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<ApiResponse>(jsonText)

        if (response.status == 200) {
            response.items
        } else {
            listOf(baseWord) // Return original word if API failed
        }
    } catch (e: Exception) {
        println("⚠️ Error fetching word forms: ${e.message}")
        listOf(baseWord)
    }
}

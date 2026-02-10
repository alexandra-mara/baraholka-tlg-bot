package com.botbot.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder

// Data class for Datamuse API
@Serializable
data class DatamuseResult(val word: String)

// Data class for htmlweb.ru API
@Serializable
data class HtmlWebResult(val status: Int, val items: List<String>)


/**
 * Gets word forms from the Datamuse API.
 */
private suspend fun getFormsFromDatamuse(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://api.datamuse.com/words?sp=$encodedWord*&v=ru"

    return try {
        val jsonText = URL(apiUrl).readText()
        Json { ignoreUnknownKeys = true }.decodeFromString<List<DatamuseResult>>(jsonText).map { it.word }
    } catch (e: Exception) {
        println("⚠️ Error fetching from Datamuse: ${e.message}")
        emptyList()
    }
}

/**
 * Gets word forms from the htmlweb.ru API.
 */
private suspend fun getFormsFromHtmlWeb(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://htmlweb.ru/json/service/inflect?inflect=$encodedWord"
    return try {
        val jsonText = URL(apiUrl).readText()
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<HtmlWebResult>(jsonText)
        if (response.status == 200) response.items else emptyList()
    } catch (e: Exception) {
        println("⚠️ Error fetching from htmlweb.ru: ${e.message}")
        emptyList()
    }
}

/**
 * Orchestrates getting word forms from multiple APIs for robustness.
 */
suspend fun getWordForms(baseWord: String): List<String> {
    // Call both APIs concurrently
    val datamuseForms = getFormsFromDatamuse(baseWord)
    val htmlWebForms = getFormsFromHtmlWeb(baseWord)

    // Combine results, add the original word, and remove duplicates
    val allForms = (datamuseForms + htmlWebForms + listOf(baseWord)).toSet().toList()
    
    return allForms.ifEmpty { listOf(baseWord) } // Ensure we never return an empty list
}

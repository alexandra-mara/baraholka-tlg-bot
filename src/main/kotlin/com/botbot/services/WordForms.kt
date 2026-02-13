package com.botbot.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder

// Data class for Datamuse API and relatedwords.org API
@Serializable
data class WordResult(val word: String)

// Data class for htmlweb.ru API, handles inconsistent `items` field
@Serializable
data class HtmlWebResult(val status: Int, val items: JsonElement)

// A single, reusable Json instance to avoid redundant creations and improve performance.
private val json = Json { ignoreUnknownKeys = true }

private suspend fun getFormsFromDatamuse(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://api.datamuse.com/words?sp=$encodedWord*&v=ru"

    return try {
        val jsonText = URI(apiUrl).toURL().readText()
        json.decodeFromString<List<WordResult>>(jsonText).map { it.word }
    } catch (e: Exception) {
        println("⚠️ Error fetching from Datamuse: ${e.message}")
        emptyList()
    }
}

private suspend fun getFormsFromHtmlWeb(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://htmlweb.ru/json/service/inflect?inflect=$encodedWord"
    return try {
        val jsonText = URI(apiUrl).toURL().readText()
        val response = json.decodeFromString<HtmlWebResult>(jsonText)
        if (response.status == 200 && response.items is JsonArray) {
            // If `items` is an array, decode it to a list of strings
            response.items.jsonArray.map { it.jsonPrimitive.content }
        } else {
            // If `items` is `false` or something else, return an empty list
            emptyList()
        }
    } catch (e: Exception) {
        println("⚠️ Error fetching from htmlweb.ru: ${e.message}")
        emptyList()
    }
}

private suspend fun getFormsFromRelatedWords(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://relatedwords.org/api/related?term=$encodedWord"
    return try {
        val jsonText = URI(apiUrl).toURL().readText()
        // Explicitly handle the empty array case for robustness.
        if (jsonText.trim() == "[]") {
            return emptyList()
        }
        // This API sometimes returns an array of objects, not an object containing an array
        json.decodeFromString<List<WordResult>>(jsonText).map { it.word }
    } catch (e: Exception) {
        println("⚠️ Error fetching from relatedwords.org: ${e.message}")
        emptyList()
    }
}

/**
 * Orchestrates getting word forms from multiple sources concurrently for better performance.
 */
suspend fun getWordForms(baseWord: String): List<String> = coroutineScope {
    val deferreds = listOf(
        async { getFormsFromDatamuse(baseWord) },
        async { getFormsFromHtmlWeb(baseWord) },
        async { getFormsFromRelatedWords(baseWord) }
    )

    val results = deferreds.awaitAll()
    // Combine results, convert to lowercase, add the original word, and remove duplicates.
    val allForms = (results.flatten() + baseWord).map { it.lowercase() }.toSet().toList()

    allForms.ifEmpty { listOf(baseWord.lowercase()) } // Ensure we never return an empty list
}

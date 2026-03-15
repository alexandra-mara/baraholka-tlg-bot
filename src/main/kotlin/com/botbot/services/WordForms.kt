package com.botbot.services

import com.botbot.utils.fetchUrl
import com.botbot.utils.withRetryAndTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
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

    return withRetryAndTimeout("Datamuse") {
        val jsonText = fetchUrl(apiUrl)
        json.decodeFromString<List<WordResult>>(jsonText).map { it.word }
    } ?: emptyList()
}

private suspend fun getFormsFromHtmlWeb(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://htmlweb.ru/json/service/inflect?inflect=$encodedWord"
    return withRetryAndTimeout("htmlweb.ru") {
        val jsonText = fetchUrl(apiUrl)
        val response = json.decodeFromString<HtmlWebResult>(jsonText)
        if (response.status == 200 && response.items is JsonArray) {
            response.items.jsonArray.map { it.jsonPrimitive.content }
        } else {
            emptyList()
        }
    } ?: emptyList()
}

private suspend fun getFormsFromRelatedWords(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://relatedwords.org/api/related?term=$encodedWord"
    return withRetryAndTimeout("relatedwords.org") {
        val jsonText = fetchUrl(apiUrl)
        if (jsonText.trim() == "[]") {
            emptyList()
        } else {
            json.decodeFromString<List<WordResult>>(jsonText).map { it.word }
        }
    } ?: emptyList()
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

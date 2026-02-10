package com.botbot.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder

// Data class for Datamuse API
@Serializable
data class DatamuseResult(val word: String)

// Data class for htmlweb.ru API
@Serializable
data class HtmlWebResult(val status: Int, val items: List<String>)

// Data class for relatedwords.org API
@Serializable
data class RelatedWordsResult(val words: List<String>)

// A single, reusable Json instance to avoid redundant creations and improve performance.
private val json = Json { ignoreUnknownKeys = true }

private suspend fun getFormsFromDatamuse(baseWord: String): List<String> {
    val encodedWord = URLEncoder.encode(baseWord, "UTF-8")
    val apiUrl = "https://api.datamuse.com/words?sp=$encodedWord*&v=ru"

    return try {
        val jsonText = URI(apiUrl).toURL().readText()
        json.decodeFromString<List<DatamuseResult>>(jsonText).map { it.word }
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
        if (response.status == 200) response.items else emptyList()
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
        json.decodeFromString<RelatedWordsResult>(jsonText).words
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
    val allForms = (results.flatten() + baseWord).toSet().toList()

    allForms.ifEmpty { listOf(baseWord) } // Ensure we never return an empty list
}

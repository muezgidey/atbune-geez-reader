package org.atbune.reader

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale

@Serializable
data class BookPage(
    val page: Int,
    val text: String,
    val review_status: String? = null,
)

@Serializable
data class BookDraft(
    val title: String,
    val language: String? = null,
    val source: String? = null,
    val status: String,
    val pages: List<BookPage>,
)

object BookParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(raw: String): BookDraft = json.decodeFromString(raw)
}

fun findMatchingPages(pages: List<BookPage>, query: String): List<BookPage> {
    val needle = normalizeSearch(query)
    if (needle.isBlank()) return emptyList()
    return pages.filter { normalizeSearch(it.text).contains(needle) }
}

private fun normalizeSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)

package com.diegocmar.booksearch.data.mapper

import com.diegocmar.booksearch.data.remote.dto.SearchDocDto
import com.diegocmar.booksearch.data.remote.dto.WorkDto
import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookDetail
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val COVERS_BASE_URL = "https://covers.openlibrary.org/b/id/"
private const val MAX_SUBJECTS = 10

internal fun SearchDocDto.toDomain(): Book = Book(
    id = key.toWorkId(),
    title = title.orEmpty(),
    authors = authorNames,
    firstPublishYear = firstPublishYear,
    editionCount = editionCount,
    coverUrl = coverId?.toCoverUrl(size = "M")
)

internal fun WorkDto.toDomain(): BookDetail = BookDetail(
    id = key.toWorkId(),
    title = title.orEmpty(),
    description = description.toDescriptionText(),
    firstPublishDate = firstPublishDate,
    subjects = subjects.take(MAX_SUBJECTS),
    coverUrl = covers.firstOrNull { it > 0 }?.toCoverUrl(size = "L")
)

/** `/works/OL45804W` -> `OL45804W` */
internal fun String.toWorkId(): String = substringAfterLast('/')

private fun Long.toCoverUrl(size: String): String? = takeIf {
    it > 0
}?.let { "$COVERS_BASE_URL$it-$size.jpg" }

private fun kotlinx.serialization.json.JsonElement?.toDescriptionText(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> this["value"]?.jsonPrimitive?.contentOrNull
    else -> null
}?.takeIf { it.isNotBlank() }

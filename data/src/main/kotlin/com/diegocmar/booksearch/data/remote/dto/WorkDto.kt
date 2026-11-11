package com.diegocmar.booksearch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WorkDto(
    @SerialName("key") val key: String,
    @SerialName("title") val title: String? = null,
    // Open Library returns either a plain string or a `{ "type": ..., "value": ... }` object.
    @SerialName("description") val description: JsonElement? = null,
    @SerialName("first_publish_date") val firstPublishDate: String? = null,
    @SerialName("subjects") val subjects: List<String> = emptyList(),
    @SerialName("covers") val covers: List<Long> = emptyList()
)

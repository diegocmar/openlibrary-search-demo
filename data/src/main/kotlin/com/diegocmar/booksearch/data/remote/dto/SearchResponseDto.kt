package com.diegocmar.booksearch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(@SerialName("docs") val docs: List<SearchDocDto> = emptyList())

@Serializable
data class SearchDocDto(
    @SerialName("key") val key: String,
    @SerialName("title") val title: String? = null,
    @SerialName("author_name") val authorNames: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("edition_count") val editionCount: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null
)

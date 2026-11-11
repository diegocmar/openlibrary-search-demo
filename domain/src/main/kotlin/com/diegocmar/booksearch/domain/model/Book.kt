package com.diegocmar.booksearch.domain.model

/**
 * A book (an Open Library "work") as shown in the search results.
 */
data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val firstPublishYear: Int?,
    val editionCount: Int?,
    val coverUrl: String?
)

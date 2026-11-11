package com.diegocmar.booksearch.domain.model

/**
 * Full information of a book, shown in the detail screen.
 */
data class BookDetail(
    val id: String,
    val title: String,
    val description: String?,
    val firstPublishDate: String?,
    val subjects: List<String>,
    val coverUrl: String?
)

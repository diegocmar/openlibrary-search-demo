package com.diegocmar.booksearch.domain.usecase

import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.repository.BookRepository
import javax.inject.Inject

/** The query is trimmed; a blank query returns an empty list without touching the repository. */
class SearchBooksUseCase
@Inject
constructor(private val repository: BookRepository) {
    /**
     * @param query free text typed by the user.
     * @return [Result.success] with the matching books, or [Result.failure] with a
     * [com.diegocmar.booksearch.domain.model.BookError].
     */
    suspend operator fun invoke(query: String): Result<List<Book>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return Result.success(emptyList())
        return repository.searchBooks(normalizedQuery)
    }
}

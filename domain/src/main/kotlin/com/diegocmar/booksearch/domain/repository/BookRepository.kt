package com.diegocmar.booksearch.domain.repository

import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookDetail

/**
 * Source of books. Implementations decide where the data comes from (network, cache...).
 *
 * Every function is main-safe and cooperative with cancellation: cancelling the caller cancels the
 * underlying work and the [kotlinx.coroutines.CancellationException] is propagated, never wrapped in a
 * [Result].
 */
interface BookRepository {
    /**
     * Searches books matching [query].
     *
     * @return [Result.success] with the matching books (possibly empty), or [Result.failure] with a
     * [com.diegocmar.booksearch.domain.model.BookError].
     */
    suspend fun searchBooks(query: String): Result<List<Book>>

    /**
     * Gets the detail of the book identified by [id].
     *
     * @return [Result.success] with the detail, or [Result.failure] with a
     * [com.diegocmar.booksearch.domain.model.BookError].
     */
    suspend fun getBookDetail(id: String): Result<BookDetail>
}

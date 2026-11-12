package com.diegocmar.booksearch.data.repository

import com.diegocmar.booksearch.data.cache.InMemoryCache
import com.diegocmar.booksearch.data.mapper.toDomain
import com.diegocmar.booksearch.data.remote.OpenLibraryApi
import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.domain.repository.BookRepository
import javax.inject.Inject

/**
 * [BookRepository] backed by the Open Library API with an in-memory, time-limited cache for searches.
 *
 * - Valid cached result -> returned without any network call.
 * - Missing or expired -> fetched from network and the cache is refreshed.
 * - Failures are never cached, so the next attempt goes to network again.
 */
class BookRepositoryImpl
@Inject
constructor(
    private val api: OpenLibraryApi,
    private val searchCache: InMemoryCache<String, List<Book>>
) : BookRepository {
    override suspend fun searchBooks(query: String): Result<List<Book>> {
        val cacheKey = query.toCacheKey()
        searchCache.get(cacheKey)?.let { return Result.success(it) }

        return safeApiCall { api.searchBooks(query.trim()).docs.map { it.toDomain() } }
            .onSuccess { searchCache.put(cacheKey, it) }
    }

    override suspend fun getBookDetail(id: String): Result<BookDetail> = safeApiCall {
        api.getDetail(id).toDomain()
    }

    /** "  Dune " and "dune" share the same cache entry. */
    private fun String.toCacheKey(): String = trim().lowercase()
}

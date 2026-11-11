package com.diegocmar.booksearch.ui

import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.domain.repository.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake repository whose searches stay "in flight" until the test completes them with [completeSearch],
 * unless [immediateSearchResult] is set. Records which searches were cancelled.
 */
class FakeBookRepository : BookRepository {
    val searchCalls = mutableListOf<String>()
    val cancelledSearches = mutableListOf<String>()
    var immediateSearchResult: ((String) -> Result<List<Book>>)? = null
    var detailResult: Result<BookDetail> = Result.success(aBookDetail())
    var detailCalls = 0

    private val pendingSearches = mutableMapOf<String, CompletableDeferred<Result<List<Book>>>>()

    override suspend fun searchBooks(query: String): Result<List<Book>> {
        searchCalls += query
        immediateSearchResult?.let { return it(query) }
        val pending = CompletableDeferred<Result<List<Book>>>().also { pendingSearches[query] = it }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            cancelledSearches += query
            throw e
        }
    }

    fun completeSearch(query: String, result: Result<List<Book>>) {
        pendingSearches.getValue(query).complete(result)
    }

    override suspend fun getBookDetail(id: String): Result<BookDetail> {
        detailCalls++
        return detailResult
    }
}

fun aBook(id: String, title: String = "Title $id") =
    Book(id, title, listOf("Author"), 2000, 1, null)

fun aBookDetail(id: String = "OL1W") =
    BookDetail(id, "Title", "Description", "2000", listOf("Fiction"), null)

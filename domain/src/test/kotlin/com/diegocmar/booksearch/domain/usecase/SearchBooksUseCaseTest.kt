package com.diegocmar.booksearch.domain.usecase

import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SearchBooksUseCaseTest {
    private val repository: BookRepository = mockk()
    private val useCase = SearchBooksUseCase(repository)

    @Test
    fun `returns the books found by the repository`() = runTest {
        val books = listOf(aBook("1"), aBook("2"))
        coEvery { repository.searchBooks("dune") } returns Result.success(books)

        val result = useCase("dune")

        assertEquals(Result.success(books), result)
    }

    @Test
    fun `trims the query before searching`() = runTest {
        coEvery { repository.searchBooks(any()) } returns Result.success(emptyList())

        useCase("  dune  ")

        coVerify(exactly = 1) { repository.searchBooks("dune") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\n\t"])
    fun `blank query returns empty list without calling the repository`(query: String) = runTest {
        val result = useCase(query)

        assertEquals(Result.success(emptyList()), result)
        coVerify(exactly = 0) { repository.searchBooks(any()) }
    }

    @Test
    fun `propagates repository errors`() = runTest {
        coEvery { repository.searchBooks(any()) } returns Result.failure(BookError.Network)

        val result = useCase("dune")

        assertEquals(BookError.Network, result.exceptionOrNull())
    }

    private fun aBook(id: String) = Book(id, "Title $id", listOf("Author"), 2000, 1, null)
}

package com.diegocmar.booksearch.domain.usecase

import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GetBookDetailUseCaseTest {
    private val repository: BookRepository = mockk()
    private val useCase = GetBookDetailUseCase(repository)

    @Test
    fun `returns the detail provided by the repository`() = runTest {
        val detail = BookDetail("OL1W", "Dune", "desc", "1965", listOf("Fiction"), null)
        coEvery { repository.getBookDetail("OL1W") } returns Result.success(detail)

        assertEquals(Result.success(detail), useCase("OL1W"))
    }

    @Test
    fun `blank id fails with NotFound without calling the repository`() = runTest {
        val result = useCase(" ")

        assertEquals(BookError.NotFound, result.exceptionOrNull())
        coVerify(exactly = 0) { repository.getBookDetail(any()) }
    }

    @Test
    fun `propagates repository errors`() = runTest {
        coEvery { repository.getBookDetail(any()) } returns
            Result.failure(BookError.Server(500))

        assertEquals(BookError.Server(500), useCase("OL1W").exceptionOrNull())
    }
}

package com.diegocmar.booksearch.data.repository

import com.diegocmar.booksearch.data.cache.InMemoryCache
import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookError
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.HttpException
import retrofit2.Response

class BookRepositoryImplTest {
    private val ttl = 3.minutes
    private val timeSource = TestTimeSource()
    private val api = FakeOpenLibraryApi()
    private val repository =
        BookRepositoryImpl(api, InMemoryCache<String, List<Book>>(ttl, timeSource))

    @Nested
    inner class SearchCache {
        @Test
        fun `first search goes to network and maps the response`() = runTest {
            val result = repository.searchBooks("dune")

            assertEquals(listOf("dune"), api.searchCalls)
            assertEquals("dune v1", result.getOrThrow().single().title)
        }

        @Test
        fun `valid cached result is returned without calling the network`() = runTest {
            val first = repository.searchBooks("dune")
            timeSource += ttl - 1.seconds

            val second = repository.searchBooks("dune")

            assertEquals(1, api.searchCalls.size)
            assertEquals(first, second)
        }

        @Test
        fun `cache key is normalized (trim and case insensitive)`() = runTest {
            repository.searchBooks("Dune")
            repository.searchBooks("  dune ")
            repository.searchBooks("DUNE")

            assertEquals(1, api.searchCalls.size)
        }

        @Test
        fun `different queries are cached independently`() = runTest {
            repository.searchBooks("dune")
            repository.searchBooks("tolkien")
            repository.searchBooks("dune")
            repository.searchBooks("tolkien")

            assertEquals(listOf("dune", "tolkien"), api.searchCalls)
        }

        @Test
        fun `expired cache fetches from network and refreshes the cache`() = runTest {
            repository.searchBooks("dune")
            api.searchAnswer = { FakeOpenLibraryApi.responseFor(it, version = 2) }
            timeSource += ttl

            val afterExpiration = repository.searchBooks("dune")
            val cachedAgain = repository.searchBooks("dune")

            assertEquals(2, api.searchCalls.size)
            assertEquals("dune v2", afterExpiration.getOrThrow().single().title)
            assertEquals(afterExpiration, cachedAgain)
        }

        @Test
        fun `failures are not cached`() = runTest {
            api.searchAnswer = { throw IOException("offline") }
            val failure = repository.searchBooks("dune")

            api.searchAnswer = { FakeOpenLibraryApi.responseFor(it) }
            val success = repository.searchBooks("dune")

            assertEquals(BookError.Network, failure.exceptionOrNull())
            assertTrue(success.isSuccess)
            assertEquals(2, api.searchCalls.size)
        }
    }

    @Nested
    inner class ErrorMapping {
        @Test
        fun `http 404 is mapped to NotFound`() = runTest {
            api.workAnswer = { throw httpException(404) }

            assertEquals(BookError.NotFound, repository.getBookDetail("OL1W").exceptionOrNull())
        }

        @Test
        fun `other http errors are mapped to Server`() = runTest {
            api.searchAnswer = { throw httpException(503) }

            assertEquals(
                BookError.Server(503),
                repository.searchBooks("dune").exceptionOrNull()
            )
        }

        @Test
        fun `cancellation is propagated instead of being turned into an error`() = runTest {
            api.searchAnswer = { throw CancellationException("cancelled") }

            assertThrows<CancellationException> { repository.searchBooks("dune") }
        }

        private fun httpException(code: Int) =
            HttpException(Response.error<Any>(code, "".toResponseBody()))
    }
}

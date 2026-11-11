package com.diegocmar.booksearch.ui.search

import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.usecase.SearchBooksUseCase
import com.diegocmar.booksearch.ui.FakeBookRepository
import com.diegocmar.booksearch.ui.MainDispatcherExtension
import com.diegocmar.booksearch.ui.aBook
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeBookRepository()

    private fun TestScope.createViewModel() = SearchViewModel(SearchBooksUseCase(repository)).also {
        runCurrent()
    }

    private fun TestScope.type(viewModel: SearchViewModel, query: String) {
        viewModel.onAction(SearchAction.QueryChanged(query))
        runCurrent()
    }

    private fun TestScope.waitDebounce() {
        advanceTimeBy(SearchViewModel.DEBOUNCE)
        runCurrent()
    }

    @Test
    fun `initial state is idle`() = runTest {
        val viewModel = createViewModel()

        assertEquals(
            SearchUiState(query = "", content = SearchContent.Idle),
            viewModel.uiState.value
        )
    }

    @Test
    fun `typing updates the query immediately and searches after the debounce`() = runTest {
        val viewModel = createViewModel()
        val books = listOf(aBook("1"))

        type(viewModel, "dune")

        assertEquals(SearchUiState("dune", SearchContent.Loading), viewModel.uiState.value)
        assertTrue(repository.searchCalls.isEmpty())

        waitDebounce()
        repository.completeSearch("dune", Result.success(books))
        runCurrent()

        assertEquals(listOf("dune"), repository.searchCalls)
        assertEquals(SearchContent.Results(books), viewModel.uiState.value.content)
    }

    @Test
    fun `fast typing only searches the last query`() = runTest {
        repository.immediateSearchResult = { Result.success(listOf(aBook(it))) }
        val viewModel = createViewModel()

        type(viewModel, "d")
        type(viewModel, "du")
        type(viewModel, "dune")
        waitDebounce()

        assertEquals(listOf("dune"), repository.searchCalls)
    }

    @Test
    fun `a new search cancels the one in flight and its results are never shown`() = runTest {
        val viewModel = createViewModel()
        val kotlinBooks = listOf(aBook("k", "Kotlin in Action"))
        val javaBooks = listOf(aBook("j", "Effective Java"))

        type(viewModel, "kotlin")
        waitDebounce()
        assertEquals(listOf("kotlin"), repository.searchCalls) // request in flight

        type(viewModel, "java")

        assertEquals(listOf("kotlin"), repository.cancelledSearches)
        assertEquals(SearchContent.Loading, viewModel.uiState.value.content)

        waitDebounce()
        repository.completeSearch("java", Result.success(javaBooks))
        runCurrent()
        // A late answer of the cancelled request has no effect.
        repository.completeSearch("kotlin", Result.success(kotlinBooks))
        runCurrent()

        assertEquals(
            SearchUiState("java", SearchContent.Results(javaBooks)),
            viewModel.uiState.value
        )
    }

    @Test
    fun `clearing the query cancels the search and goes back to idle`() = runTest {
        val viewModel = createViewModel()
        type(viewModel, "dune")
        waitDebounce()

        viewModel.onAction(SearchAction.ClearQuery)
        runCurrent()

        assertEquals(listOf("dune"), repository.cancelledSearches)
        assertEquals(SearchUiState("", SearchContent.Idle), viewModel.uiState.value)
    }

    @Test
    fun `whitespace-only changes do not trigger a new search`() = runTest {
        repository.immediateSearchResult = { Result.success(listOf(aBook(it))) }
        val viewModel = createViewModel()

        type(viewModel, "dune")
        waitDebounce()
        type(viewModel, "dune ")
        waitDebounce()

        assertEquals(listOf("dune"), repository.searchCalls)
        assertEquals("dune ", viewModel.uiState.value.query)
    }

    @Test
    fun `no results shows the empty state`() = runTest {
        repository.immediateSearchResult = { Result.success(emptyList()) }
        val viewModel = createViewModel()

        type(viewModel, "zzzz")
        waitDebounce()

        assertEquals(SearchContent.Empty("zzzz"), viewModel.uiState.value.content)
    }

    @Test
    fun `errors are shown and retry searches the same query again`() = runTest {
        repository.immediateSearchResult = { Result.failure(BookError.Network) }
        val viewModel = createViewModel()
        type(viewModel, "dune")
        waitDebounce()

        assertEquals(SearchContent.Error(BookError.Network), viewModel.uiState.value.content)

        val books = listOf(aBook("1"))
        repository.immediateSearchResult = { Result.success(books) }
        viewModel.onAction(SearchAction.Retry)
        runCurrent()

        assertEquals(listOf("dune", "dune"), repository.searchCalls)
        assertEquals(SearchContent.Results(books), viewModel.uiState.value.content)
    }
}

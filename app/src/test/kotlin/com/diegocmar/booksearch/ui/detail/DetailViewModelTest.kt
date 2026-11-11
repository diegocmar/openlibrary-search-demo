package com.diegocmar.booksearch.ui.detail

import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.usecase.GetBookDetailUseCase
import com.diegocmar.booksearch.ui.FakeBookRepository
import com.diegocmar.booksearch.ui.MainDispatcherExtension
import com.diegocmar.booksearch.ui.aBookDetail
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeBookRepository()

    private fun createViewModel() = DetailViewModel("OL1W", GetBookDetailUseCase(repository))

    @Test
    fun `starts loading and then shows the detail`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DetailUiState.Loading, viewModel.uiState.value)

        runCurrent()

        assertEquals(DetailUiState.Success(aBookDetail()), viewModel.uiState.value)
    }

    @Test
    fun `shows the error and retry loads again`() = runTest {
        repository.detailResult = Result.failure(BookError.Network)
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(DetailUiState.Error(BookError.Network), viewModel.uiState.value)

        repository.detailResult = Result.success(aBookDetail())
        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.detailCalls)
        assertEquals(DetailUiState.Success(aBookDetail()), viewModel.uiState.value)
    }
}

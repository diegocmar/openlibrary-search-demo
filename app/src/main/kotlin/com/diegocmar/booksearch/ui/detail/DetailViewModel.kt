package com.diegocmar.booksearch.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.usecase.GetBookDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel
@AssistedInject
constructor(
    @Assisted private val bookId: String,
    private val getBookDetail: GetBookDetailUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _uiState.value = DetailUiState.Loading
                _uiState.value =
                    getBookDetail(bookId).fold(
                        onSuccess = { DetailUiState.Success(it) },
                        onFailure = {
                            DetailUiState.Error(it as? BookError ?: BookError.Unknown(it))
                        }
                    )
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(bookId: String): DetailViewModel
    }
}

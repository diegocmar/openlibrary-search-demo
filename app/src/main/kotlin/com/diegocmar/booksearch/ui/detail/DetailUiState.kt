package com.diegocmar.booksearch.ui.detail

import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.domain.model.BookError

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Success(val book: BookDetail) : DetailUiState

    data class Error(val error: BookError) : DetailUiState
}

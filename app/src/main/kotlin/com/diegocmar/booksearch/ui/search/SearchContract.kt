package com.diegocmar.booksearch.ui.search

import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.model.BookError

/**
 * Immutable state of the search screen. [query] is updated synchronously on every keystroke so the
 * text field never lags; [content] reflects the result of the latest search only.
 */
data class SearchUiState(val query: String = "", val content: SearchContent = SearchContent.Idle)

sealed interface SearchContent {
    /** Nothing typed yet. */
    data object Idle : SearchContent

    data object Loading : SearchContent

    data class Empty(val query: String) : SearchContent

    data class Results(val books: List<Book>) : SearchContent

    data class Error(val error: BookError) : SearchContent
}

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction

    data object ClearQuery : SearchAction

    data object Retry : SearchAction
}

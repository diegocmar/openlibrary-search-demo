package com.diegocmar.booksearch.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.usecase.SearchBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Search screen ViewModel.
 *
 * Cancellation: every search request goes through [flatMapLatest]. When a new request arrives, the
 * coroutine of the previous one is cancelled — both while it is waiting for the debounce and while
 * the HTTP call is in flight (Retrofit cancels the OkHttp call). A cancelled search never emits, so
 * results of an older query can never overwrite the newer one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
@Inject
constructor(private val searchBooks: SearchBooksUseCase) :
    ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queries = MutableStateFlow("")
    private val retries =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    init {
        searchRequests()
            .flatMapLatest { request -> search(request) }
            .onEach { content -> _uiState.update { it.copy(content = content) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> updateQuery(action.query)
            SearchAction.ClearQuery -> updateQuery("")
            SearchAction.Retry -> retries.tryEmit(Unit)
        }
    }

    private fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        queries.value = query
    }

    private fun searchRequests(): Flow<SearchRequest> = merge(
        queries
            .map { it.trim() }
            .distinctUntilChanged()
            .map { SearchRequest(query = it, debounce = true) },
        retries.map { SearchRequest(query = queries.value.trim(), debounce = false) }
    )

    private fun search(request: SearchRequest): Flow<SearchContent> {
        if (request.query.isEmpty()) return flowOf(SearchContent.Idle)
        return flow {
            // Results of the previous query are hidden right away, not when the new ones arrive.
            emit(SearchContent.Loading)
            if (request.debounce) delay(DEBOUNCE)
            val content =
                searchBooks(request.query).fold(
                    onSuccess = { books ->
                        if (books.isEmpty()) {
                            SearchContent.Empty(
                                request.query
                            )
                        } else {
                            SearchContent.Results(books)
                        }
                    },
                    onFailure = { error ->
                        SearchContent.Error(error as? BookError ?: BookError.Unknown(error))
                    }
                )
            emit(content)
        }
    }

    private data class SearchRequest(val query: String, val debounce: Boolean)

    companion object {
        val DEBOUNCE = 350.milliseconds
    }
}

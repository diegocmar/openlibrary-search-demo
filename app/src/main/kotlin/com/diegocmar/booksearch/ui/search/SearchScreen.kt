package com.diegocmar.booksearch.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegocmar.booksearch.R
import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.ui.common.CoverImage
import com.diegocmar.booksearch.ui.common.ErrorContent
import com.diegocmar.booksearch.ui.common.LoadingContent
import com.diegocmar.booksearch.ui.common.MessageContent
import com.diegocmar.booksearch.ui.theme.BookSearchTheme

@Composable
fun SearchRoute(onBookClick: (Book) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(uiState = uiState, onAction = viewModel::onAction, onBookClick = onBookClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onBookClick: (Book) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.search_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = uiState.query,
                onQueryChange = { onAction(SearchAction.QueryChanged(it)) },
                onClear = { onAction(SearchAction.ClearQuery) }
            )
            when (val content = uiState.content) {
                SearchContent.Idle -> MessageContent(stringResource(R.string.search_idle))

                SearchContent.Loading -> LoadingContent()

                is SearchContent.Empty -> MessageContent(
                    stringResource(R.string.search_empty, content.query)
                )

                is SearchContent.Error -> ErrorContent(content.error, onRetry = {
                    onAction(SearchAction.Retry)
                })

                is SearchContent.Results -> BookList(
                    books = content.books,
                    onBookClick = onBookClick
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_clear)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
    )
}

@Composable
private fun BookList(books: List<Book>, onBookClick: (Book) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items = books, key = { it.id }, contentType = { "book" }) { book ->
            BookRow(book = book, onClick = { onBookClick(book) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun BookRow(book: Book, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(
            onClick = onClick
        ).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            url = book.coverUrl,
            contentScale = ContentScale.Crop,
            loaderSize = 20.dp,
            modifier = Modifier.size(width = 48.dp, height = 72.dp).clip(RoundedCornerShape(4.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.authors.joinToString().ifEmpty {
                    stringResource(R.string.book_unknown_author)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            book.firstPublishYear?.let {
                Text(
                    text = stringResource(R.string.book_first_published, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    BookSearchTheme {
        SearchScreen(
            uiState =
                SearchUiState(
                    query = "dune",
                    content =
                        SearchContent.Results(
                            listOf(
                                Book("OL893414W", "Dune", listOf("Frank Herbert"), 1965, 155, null),
                                Book(
                                    "OL893461W",
                                    "Dune Messiah",
                                    listOf("Frank Herbert"),
                                    1969,
                                    101,
                                    null
                                )
                            )
                        )
                ),
            onAction = {},
            onBookClick = {}
        )
    }
}

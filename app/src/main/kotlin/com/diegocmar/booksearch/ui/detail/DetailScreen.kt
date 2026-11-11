package com.diegocmar.booksearch.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegocmar.booksearch.R
import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.ui.common.CoverImage
import com.diegocmar.booksearch.ui.common.ErrorContent
import com.diegocmar.booksearch.ui.common.LoadingContent
import com.diegocmar.booksearch.ui.theme.BookSearchTheme
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun DetailRoute(
    bookId: String,
    title: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel =
        hiltViewModel<DetailViewModel, DetailViewModel.Factory> { it.create(bookId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(title = title, uiState = uiState, onBack = onBack, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(title: String, uiState: DetailUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            DetailUiState.Loading -> LoadingContent(modifier)

            is DetailUiState.Error -> ErrorContent(
                uiState.error,
                onRetry = onRetry,
                modifier = modifier
            )

            is DetailUiState.Success -> BookDetailContent(uiState.book, modifier)
        }
    }
}

@Composable
private fun BookDetailContent(book: BookDetail, modifier: Modifier = Modifier) {
    val notAvailable = stringResource(R.string.detail_not_available)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        book.coverUrl?.let {
            CoverImage(
                url = it,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .align(Alignment.CenterHorizontally)
            )
        }
        Text(book.title, style = MaterialTheme.typography.headlineSmall)
        DetailField(
            stringResource(R.string.detail_first_publish_date),
            book.firstPublishDate ?: notAvailable
        )
        DetailField(
            stringResource(R.string.detail_subjects),
            book.subjects.joinToString().ifEmpty {
                notAvailable
            }
        )
        DetailField(stringResource(R.string.detail_description)) {
            book.description?.let { MarkdownText(it) }
                ?: Text(notAvailable, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    DetailField(label) { Text(value, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun DetailField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    Markdown(
        content = markdown,
        typography = markdownTypography(paragraph = MaterialTheme.typography.bodyMedium),
        imageTransformer = Coil3ImageTransformerImpl
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPreview() {
    BookSearchTheme {
        DetailScreen(
            title = "Dune",
            uiState =
                DetailUiState.Success(
                    BookDetail(
                        id = "OL893414W",
                        title = "Dune",
                        description =
                            "Set on the desert planet **Arrakis**.\n\n" +
                                "- [Dune Messiah](https://openlibrary.org/works/OL893526W)",
                        firstPublishDate = "1965",
                        subjects = listOf("Science fiction", "Dune (Imaginary place)"),
                        coverUrl = null
                    )
                ),
            onBack = {},
            onRetry = {}
        )
    }
}

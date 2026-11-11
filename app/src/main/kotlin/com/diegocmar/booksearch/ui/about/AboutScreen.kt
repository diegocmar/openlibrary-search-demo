package com.diegocmar.booksearch.ui.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diegocmar.booksearch.R
import com.diegocmar.booksearch.ui.common.LoadingContent
import com.diegocmar.booksearch.ui.common.MarkdownText
import com.diegocmar.booksearch.ui.theme.BookSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val README_ASSET = "README.md"

@Composable
fun AboutRoute(onBack: () -> Unit) {
    val assets = LocalContext.current.assets
    val readme by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            assets.open(README_ASSET).bufferedReader().use { it.readText() }
        }
    }
    AboutScreen(markdown = readme, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(markdown: String?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        if (markdown == null) {
            LoadingContent(modifier)
        } else {
            MarkdownText(
                markdown = markdown,
                modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    BookSearchTheme {
        AboutScreen(
            markdown = "# Book Search\n\nApp that searches books in **Open Library**.",
            onBack = {}
        )
    }
}

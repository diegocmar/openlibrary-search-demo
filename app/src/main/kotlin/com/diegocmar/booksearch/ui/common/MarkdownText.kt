package com.diegocmar.booksearch.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Markdown(
        content = markdown,
        typography = markdownTypography(paragraph = MaterialTheme.typography.bodyMedium),
        imageTransformer = Coil3ImageTransformerImpl,
        modifier = modifier
    )
}

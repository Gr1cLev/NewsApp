package com.example.newsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.ui.theme.BackgroundGray
import com.example.newsapp.ui.theme.DividerColor
import com.example.newsapp.ui.theme.TextPrimary
import com.example.newsapp.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    suggestions: List<String>,
    articles: List<NewsArticle>
) {
    var query by remember { mutableStateOf("") }

    val filteredArticles = remember(query, articles) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) emptyList()
        else articles.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                    it.summary.contains(trimmed, ignoreCase = true) ||
                    it.category.contains(trimmed, ignoreCase = true)
        }
    }

    val filteredSuggestions = remember(query, suggestions) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) suggestions.take(5)
        else suggestions.filter { it.contains(trimmed, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Try \"World news\" or \"Tech\"") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (query.isBlank()) "Suggested searches" else "Results",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (query.isBlank()) {
            SuggestionList(suggestions = filteredSuggestions, onSuggestionSelected = { query = it })
        } else {
            SearchResults(results = filteredArticles)
        }
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(suggestions) { suggestion ->
            ElevatedCard(
                onClick = { onSuggestionSelected(suggestion) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RowHeadlineWithIcon(text = suggestion)
                }
            }
        }
    }
}

@Composable
private fun RowHeadlineWithIcon(text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchResults(results: List<NewsArticle>) {
    if (results.isEmpty()) {
        Text(
            text = "No articles matched your search yet. Try another keyword.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(results, key = { it.id }) { article ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${article.source} - ${article.publishedAt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DividerColor
                    )
                }
            }
        }
    }
}


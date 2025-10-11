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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.ui.theme.BackgroundGray
import com.example.newsapp.ui.theme.DividerColor
import com.example.newsapp.ui.theme.PrimaryBlue
import com.example.newsapp.ui.theme.TextPrimary
import com.example.newsapp.ui.theme.TextSecondary

@Composable
fun BookmarksScreen(
    bookmarks: List<NewsArticle>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Bookmarks",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (bookmarks.isEmpty()) {
            EmptyBookmarks()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(bookmarks, key = { it.id }) { article ->
                    BookmarkCard(article = article)
                }
            }
        }
    }
}

@Composable
private fun EmptyBookmarks() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmarks,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier
                .size(48.dp)
        )
        Text(
            text = "No articles saved yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Text(
            text = "Tap the bookmark icon on news stories to read them later.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BookmarkCard(article: NewsArticle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = article.category,
                style = MaterialTheme.typography.labelMedium.copy(color = PrimaryBlue)
            )
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
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


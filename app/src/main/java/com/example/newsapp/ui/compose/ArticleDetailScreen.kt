package com.example.newsapp.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: Int,
    onBack: () -> Unit,
    onBookmarkChanged: () -> Unit
) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val article by produceState<NewsArticle?>(initialValue = null, articleId) {
        value = withContext(Dispatchers.IO) { NewsRepository.getArticleById(context, articleId) }
    }

    LaunchedEffect(article) {
        article?.let {
            isBookmarked = withContext(Dispatchers.IO) {
                NewsRepository.isArticleBookmarked(context, it.id)
            }
        }
    }

    if (article == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Artikel tidak ditemukan.",
                style = MaterialTheme.typography.bodyLarge
            )
            FilledTonalButton(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onBack
            ) {
                Text("Kembali")
            }
        }
        return
    }

    val currentArticle = article!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        HeroHeader(article = currentArticle)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = currentArticle.tag.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentArticle.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${currentArticle.publishedAt} • ${currentArticle.source}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AiInsightSection(article = currentArticle)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val paragraphs = currentArticle.content.takeIf { it.isNotEmpty() }
                    ?: listOf(currentArticle.summary)
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            ActionRow(
                isBookmarked = isBookmarked,
                onBookmarkToggle = {
                    coroutineScope.launch {
                        val bookmarked = withContext(Dispatchers.IO) {
                            NewsRepository.toggleBookmark(context, currentArticle.id)
                        }
                        isBookmarked = bookmarked
                        onBookmarkChanged()
                        Toast.makeText(
                            context,
                            context.getString(
                                if (bookmarked) R.string.bookmark_added else R.string.bookmark_removed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun HeroHeader(article: NewsArticle) {
    val accentColor = Color(article.accentColor)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(article.heroImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = article.title,
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f)),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiInsightSection(article: NewsArticle) {
    val labels = remember(article) { generateAiTags(article) }
    if (labels.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.article_ai_section_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { label ->
                AssistChip(
                    onClick = {},
                    label = { Text(label) },
                    shape = RoundedCornerShape(18.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit
) {
    val context = LocalContext.current
    val bookmarkLabel = stringResource(
        if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onBookmarkToggle,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = bookmarkLabel,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = bookmarkLabel,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        FilledTonalButton(
            onClick = {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_download_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_download_news))
        }
    }
}

private fun generateAiTags(article: NewsArticle): List<String> {
    val tags = mutableListOf<String>()
    tags += "Sedang tren"
    when (article.category.lowercase(Locale.getDefault())) {
        "olahraga", "sport" -> {
            tags += listOf("Favorit fans", "Analisis laga")
        }
        "bisnis", "business" -> {
            tags += listOf("Briefing pagi", "Radar pasar")
        }
        "teknologi", "tech" -> {
            tags += listOf("Detak inovasi", "Sorotan fitur")
        }
        else -> tags += "Sorotan khusus"
    }
    if (article.summary.contains("analysis", ignoreCase = true) ||
        article.summary.contains("analisis", ignoreCase = true)
    ) {
        tags += "Ulasan mendalam"
    }
    return tags.distinct().take(5)
}

package com.example.newsapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.ui.theme.BackgroundGray
import com.example.newsapp.ui.theme.DividerColor
import com.example.newsapp.ui.theme.PrimaryBlue
import com.example.newsapp.ui.theme.PrimaryBlueDark
import com.example.newsapp.ui.theme.PrimaryBlueLight
import com.example.newsapp.ui.theme.TextPrimary
import com.example.newsapp.ui.theme.TextSecondary

@Composable
fun NewsHomeScreen(
    categories: List<NewsCategory>,
    featuredArticles: List<NewsArticle>,
    allArticles: List<NewsArticle>,
    contentPadding: PaddingValues
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "All") }
    val filteredArticles = remember(selectedCategory, allArticles) {
        if (selectedCategory == "All") allArticles else allArticles.filter { it.category.equals(selectedCategory, true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            FeaturedSection(articles = featuredArticles)
        }

        item {
            CategoryRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        items(filteredArticles, key = { it.id }) { article ->
            ArticleCard(article = article)
        }
    }
}

@Composable
private fun FeaturedSection(
    articles: List<NewsArticle>
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(articles, key = { it.id }) { article ->
            FeaturedArticleCard(article = article)
        }
    }
}

@Composable
private fun FeaturedArticleCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(200.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            article.accentColor.copy(alpha = 0.85f),
                            PrimaryBlueDark
                        )
                    )
                )
                .fillMaxWidth()
                .height(200.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Badge(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    contentColor = PrimaryBlue
                ) {
                    Text(
                        text = article.tag.uppercase(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<NewsCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            val selected = category.name.equals(selectedCategory, ignoreCase = true)
            val chipColor by animateColorAsState(
                targetValue = if (selected) PrimaryBlue else MaterialTheme.colorScheme.surface,
                label = "ChipColor"
            )
            AssistChip(
                onClick = { onCategorySelected(category.name) },
                label = {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipColor,
                    labelColor = if (selected) Color.White else TextSecondary
                ),
                border = null
            )
        }
    }
}

@Composable
private fun ArticleCard(
    article: NewsArticle
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                article.accentColor.copy(alpha = 0.85f),
                                article.accentColor.copy(alpha = 0.5f),
                                PrimaryBlueLight
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = article.category.lowercase(),
                    style = MaterialTheme.typography.labelMedium.copy(color = PrimaryBlueDark),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Spacer(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(DividerColor)
                    )
                    Text(
                        text = article.publishedAt,
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}

package com.example.newsapp.ui.compose

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.example.newsapp.data.UserPreferences
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.InputStream
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.model.NewsData
import com.example.newsapp.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class ProfileLoadState(
    val loaded: Boolean,
    val profile: UserProfile?
)

private enum class HomeTab(
    val label: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    News("Berita", Icons.Filled.Article, Icons.Outlined.Article),
    Search("Cari", Icons.Filled.Search, Icons.Outlined.Search),
    Bookmarks("Tersimpan", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    Profile("Profil", Icons.Filled.Person, Icons.Outlined.PersonOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bookmarksVersion: Int,
    profileVersion: Int,
    onOpenArticle: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onRequireAuthentication: () -> Unit,
    onBookmarksUpdated: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.News) }
    val coroutineScope = rememberCoroutineScope()

    val newsData by produceState<NewsData?>(initialValue = null, bookmarksVersion) {
        value = withContext(Dispatchers.IO) { NewsRepository.getNewsData(context) }
    }
    val bookmarks by produceState<List<NewsArticle>>(initialValue = emptyList(), bookmarksVersion) {
        value = withContext(Dispatchers.IO) { NewsRepository.getBookmarks(context) }
    }
    val profileState by produceState(initialValue = ProfileLoadState(loaded = false, profile = null), profileVersion) {
        val result = withContext(Dispatchers.IO) { ProfileRepository.getActiveProfile(context) }
        value = ProfileLoadState(loaded = true, profile = result)
    }
    val profileLoaded = profileState.loaded
    val profile = profileState.profile
    val suggestions by produceState<List<String>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { NewsRepository.getSearchSuggestions(context) }
    }

    LaunchedEffect(profileVersion, profileLoaded, profile) {
        if (profileLoaded && profile == null) {
            onRequireAuthentication()
        }
    }

    val bookmarkedIds = remember(bookmarks) { bookmarks.mapTo(mutableSetOf()) { it.id } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Membuat Scaffold transparan
        topBar = {
            HomeTopBar(
                tab = selectedTab,
                profile = profile,
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (tab == selectedTab) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.News -> NewsTab(
                modifier = Modifier.padding(innerPadding),
                newsData = newsData,
                bookmarkedIds = bookmarkedIds,
                onBookmarkToggle = { article ->
                    coroutineScope.launch {
                        val isBookmarked = withContext(Dispatchers.IO) {
                            NewsRepository.toggleBookmark(context, article.id)
                        }
                        onBookmarksUpdated()
                        Toast.makeText(
                            context,
                            context.getString(if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onArticleClick = { onOpenArticle(it.id) }
            )

            HomeTab.Search -> SearchTab(
                modifier = Modifier.padding(innerPadding),
                articles = newsData?.articles.orEmpty(),
                suggestions = suggestions,
                bookmarkedIds = bookmarkedIds,
                onBookmarkToggle = { article ->
                    coroutineScope.launch {
                        val isBookmarked = withContext(Dispatchers.IO) {
                            NewsRepository.toggleBookmark(context, article.id)
                        }
                        onBookmarksUpdated()
                        Toast.makeText(
                            context,
                            context.getString(if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onArticleClick = { onOpenArticle(it.id) }
            )

            HomeTab.Bookmarks -> BookmarksTab(
                modifier = Modifier.padding(innerPadding),
                bookmarks = bookmarks,
                onArticleClick = { onOpenArticle(it.id) },
                onRemoveBookmark = { article ->
                    coroutineScope.launch {
                        val isBookmarked = withContext(Dispatchers.IO) {
                            NewsRepository.toggleBookmark(context, article.id)
                        }
                        onBookmarksUpdated()
                        Toast.makeText(
                            context,
                            context.getString(if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

            HomeTab.Profile -> ProfileTab(
                modifier = Modifier.padding(innerPadding),
                profile = profile,
                profileLoaded = profileLoaded,
                onOpenSettings = onOpenSettings,
                onRequireAuthentication = onRequireAuthentication
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    tab: HomeTab,
    profile: UserProfile?,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            Text(
                text = when (tab) {
                    HomeTab.News -> "Berita Hari Ini"
                    HomeTab.Search -> "Cari Berita"
                    HomeTab.Bookmarks -> "Tersimpan"
                    HomeTab.Profile -> profile?.fullName().takeUnless { it.isNullOrBlank() } ?: "Profil"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            if (tab == HomeTab.Profile) {
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Pengaturan")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun NewsTab(
    modifier: Modifier,
    newsData: NewsData?,
    bookmarkedIds: Set<Int>,
    onBookmarkToggle: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    TransparentBackground(modifier = modifier) {
        if (newsData == null) {
            LoadingState(Modifier)
            return@TransparentBackground
        }

        var selectedCategory by remember(newsData) {
            mutableStateOf(newsData.categories.firstOrNull()?.name ?: "Semua")
        }

        val filteredArticles = remember(selectedCategory, newsData) {
            if (selectedCategory.equals("Semua", ignoreCase = true)) {
                newsData.articles
            } else {
                newsData.articles.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (newsData.featuredArticles.isNotEmpty()) {
            item { SectionTitle("Sorotan Utama") }
            item {
                FeaturedCarousel(
                    articles = newsData.featuredArticles,
                    onArticleClick = onArticleClick
                )
            }
        }

        if (newsData.categories.isNotEmpty()) {
            item {
                CategoryRow(
                    categories = newsData.categories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it.name }
                )
            }
        }

        item { SectionTitle("Untuk Anda") }

        items(filteredArticles) { article ->
            ArticleCard(
                article = article,
                isBookmarked = bookmarkedIds.contains(article.id),
                showCategory = true,
                onBookmarkToggle = { onBookmarkToggle(article) },
                onClick = { onArticleClick(article) }
            )
        }
        }
    }
}

@Composable
private fun SearchTab(
    modifier: Modifier,
    articles: List<NewsArticle>,
    suggestions: List<String>,
    bookmarkedIds: Set<Int>,
    onBookmarkToggle: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val results = remember(trimmedQuery, articles) {
        if (trimmedQuery.isBlank()) emptyList() else {
            articles.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                    it.summary.contains(trimmedQuery, ignoreCase = true) ||
                    it.category.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val recommendations = remember(articles) {
        articles.filter { it.category.equals("Olahraga", ignoreCase = true) }.ifEmpty {
            articles.take(5)
        }
    }

    TransparentBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            value = query,
            onValueChange = { query = it },
            suggestions = suggestions
        )

        AnimatedVisibility(
            visible = trimmedQuery.isBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Rekomendasi")
                recommendations.forEach { article ->
                    ArticleCard(
                        article = article,
                        isBookmarked = bookmarkedIds.contains(article.id),
                        showCategory = true,
                        onBookmarkToggle = { onBookmarkToggle(article) },
                        onClick = { onArticleClick(article) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = trimmedQuery.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Hasil untuk \"$trimmedQuery\"")
                if (results.isEmpty()) {
                    Text(
                        text = "Belum ada berita yang cocok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    results.forEach { article ->
                        ArticleCard(
                            article = article,
                            isBookmarked = bookmarkedIds.contains(article.id),
                            showCategory = true,
                            onBookmarkToggle = { onBookmarkToggle(article) },
                            onClick = { onArticleClick(article) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BookmarksTab(
    modifier: Modifier,
    bookmarks: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit,
    onRemoveBookmark: (NewsArticle) -> Unit
) {
    TransparentBackground(modifier = modifier) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada artikel tersimpan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@TransparentBackground
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        items(bookmarks) { article ->
            ArticleCard(
                article = article,
                isBookmarked = true,
                showCategory = true,
                onBookmarkToggle = { onRemoveBookmark(article) },
                onClick = { onArticleClick(article) }
            )
        }
        }
    }
}

@Composable
private fun ProfileTab(
    modifier: Modifier,
    profile: UserProfile?,
    profileLoaded: Boolean,
    onOpenSettings: () -> Unit,
    onRequireAuthentication: () -> Unit
) {
    TransparentBackground(modifier = modifier) {
        if (!profileLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@TransparentBackground
        }

        if (profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Masuk untuk mengakses profil Anda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRequireAuthentication) {
                        Text("Masuk")
                    }
                }
            }
            return@TransparentBackground
        }

        val context = LocalContext.current
        val notificationsEnabled = UserPreferences.isNotificationsEnabled(context)
        val nightModeEnabled = UserPreferences.isNightModeEnabled(context)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.avatarInitials(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = profile.fullName().ifBlank { profile.email.substringBefore("@") },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PreferenceRow(
                    title = "Notifikasi",
                    value = if (notificationsEnabled) "Aktif" else "Nonaktif"
                )
                PreferenceRow(
                    title = "Mode malam",
                    value = if (nightModeEnabled) "Aktif" else "Nonaktif"
                )
            }
        }

        ElevatedCard(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Atur preferensi, ubah profil, atau keluar akun.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryRow(
    categories: List<NewsCategory>,
    selected: String,
    onSelect: (NewsCategory) -> Unit
) {
    // Menggunakan MaterialTheme untuk deteksi dark mode yang lebih reliable dan reactive
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(categories) { category ->
            val isSelected = category.name.equals(selected, ignoreCase = true)
            AssistChip(
                onClick = { onSelect(category) },
                label = { Text(category.name) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) {
                        // Untuk dark mode: gunakan primary dengan opacity lebih tinggi agar kontras dengan overlay hitam
                        // Untuk light mode: tetap menggunakan opacity rendah
                        if (isDarkMode) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }
    }
}

@Composable
private fun FeaturedCarousel(
    articles: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(articles) { article ->
            FeaturedArticleCard(article = article) {
                onArticleClick(article)
            }
        }
    }
}

@Composable
private fun FeaturedArticleCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .size(width = 280.dp, height = 320.dp)
    ) {
        Box {
            val accent = Color(article.accentColor)

            if (!article.heroImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.heroImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accent)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.2f),
                                accent.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = article.tag.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    showCategory: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit
) {
    val cardBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!article.heroImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(article.heroImageUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showCategory) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = article.publishedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Hapus simpanan" else "Simpan artikel",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Cari berita, kategori, atau sumber") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (value.isBlank() && suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.take(3).forEach { suggestion ->
                    AssistChip(
                        onClick = { onValueChange(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransparentBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Menggunakan MaterialTheme untuk deteksi dark mode yang lebih reliable dan reactive
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background image dengan transparansi - menggunakan BitmapFactory untuk raw resource
        val bitmap = remember {
            try {
                context.resources.openRawResource(R.raw.imagebghome).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }
        }
        
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.4f), // Alpha 40% untuk visibilitas yang baik
            )
        } else {
            // Fallback jika gambar tidak bisa dimuat
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
            )
        }
        
        // Overlay transparan dengan warna sesuai tema
        // Light mode: putih transparan, Dark mode: hitam transparan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDarkMode) {
                        // Dark mode: hitam transparan dengan opacity lebih tinggi
                        Color.Black.copy(alpha = 0.65f)
                    } else {
                        // Light mode: putih transparan
                        Color.White.copy(alpha = 0.55f)
                    }
                )
        )
        // Konten di atas background
        content()
    }
}

private fun UserProfile.avatarInitials(): String {
    val default = email.substringBefore("@")
    val source = fullName().takeIf { it.isNotBlank() } ?: default
    return source
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { default.take(2) }
        .uppercase(Locale.getDefault())
}

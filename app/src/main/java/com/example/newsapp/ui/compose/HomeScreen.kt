package com.example.newsapp.ui.compose

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newsapp.data.UserPreferences
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.model.NewsData
import com.example.newsapp.model.UserProfile
import com.example.newsapp.viewmodel.AuthState
import com.example.newsapp.viewmodel.BookmarkViewModel
import com.example.newsapp.viewmodel.NewsUiState
import com.example.newsapp.viewmodel.NewsViewModel
import com.example.newsapp.viewmodel.ProfileViewModel
import com.example.newsapp.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.example.newsapp.ui.compose.rememberBackgroundBitmap
import com.example.newsapp.ui.compose.imageAlphaForLevel

private data class ProfileLoadState(
    val loaded: Boolean,
    val profile: UserProfile?
)

private enum class HomeTab(
    val label: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    News("News", Icons.Filled.Article, Icons.Outlined.Article),
    Search("Search", Icons.Filled.Search, Icons.Outlined.Search),
    Bookmarks("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    newsRepository: NewsRepository,
    bookmarksVersion: Int,
    profileVersion: Int,
    onOpenArticle: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onRequireAuthentication: () -> Unit,
    onBookmarksUpdated: () -> Unit
) {
    // Get ViewModels
    val newsViewModel: NewsViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.News) }
    val coroutineScope = rememberCoroutineScope()

    // Collect states from ViewModels
    val newsUiState by newsViewModel.uiState.collectAsState()
    val selectedCategory by newsViewModel.selectedCategory.collectAsState()
    val categoryArticles by newsViewModel.categoryArticles.collectAsState()
    val isFetchingCategory by newsViewModel.isFetchingCategory.collectAsState()
    val isRefreshing by newsViewModel.isRefreshing.collectAsState()
    val personalizedFeatured by newsViewModel.personalizedFeatured.collectAsState()
    val personalizedArticles by newsViewModel.personalizedArticles.collectAsState()
    
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()
    
    val bookmarkedArticles by bookmarkViewModel.bookmarkedArticles.collectAsState()
    val bookmarkedIds by bookmarkViewModel.bookmarkedIds.collectAsState()
    
    val authState by profileViewModel.authState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    // Refresh bookmarks when version changes
    LaunchedEffect(bookmarksVersion) {
        bookmarkViewModel.refresh()
    }

    // Refresh profile when version changes
    LaunchedEffect(profileVersion) {
        profileViewModel.refresh()
    }
    
    // Handle auth state - redirect if not authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Guest && selectedTab == HomeTab.Profile) {
            onRequireAuthentication()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            HomeTopBar(
                tab = selectedTab,
                profile = userProfile,
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
        // Handle UI state from ViewModel
        when (val state = newsUiState) {
            is NewsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading news...")
                    }
                }
            }
            is NewsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "❌ Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            newsViewModel.refreshNews()
                            bookmarkViewModel.refresh()
                        }) {
                            Text("Try Again")
                        }
                    }
                }
            }
            is NewsUiState.Success -> {
                // Render content based on selected tab
                when (selectedTab) {
                    HomeTab.News -> NewsTab(
                        modifier = Modifier.padding(innerPadding),
                        newsViewModel = newsViewModel,
                        newsData = state,
                        categoryArticles = categoryArticles,
                        personalizedFeatured = personalizedFeatured,
                        personalizedArticles = personalizedArticles,
                        selectedCategory = selectedCategory,
                        isFetchingCategory = isFetchingCategory,
                        isRefreshing = isRefreshing,
                        bookmarkedIds = bookmarkedIds,
                        onBookmarkToggle = { article ->
                            coroutineScope.launch {
                                val wasBookmarked = bookmarkViewModel.isArticleBookmarked(article.id)
                                bookmarkViewModel.toggleBookmark(article.id)
                                onBookmarksUpdated()
                                
                                // Track bookmark for ML (only when adding bookmark)
                                if (!wasBookmarked) {
                                    searchViewModel.onArticleBookmarked(article)
                                }
                                
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        if (!wasBookmarked)
                                            R.string.bookmark_added
                                        else
                                            R.string.bookmark_removed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onArticleClick = { article ->
                            // Track click for ML
                            searchViewModel.onArticleClicked(article)
                            onOpenArticle(article.id)
                        }
                    )
                    HomeTab.Search -> SearchTab(
                        modifier = Modifier.padding(innerPadding),
                        searchViewModel = searchViewModel,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        allArticles = state.articles,
                        bookmarkedIds = bookmarkedIds,
                        onBookmarkToggle = { article ->
                            coroutineScope.launch {
                                val wasBookmarked = bookmarkViewModel.isArticleBookmarked(article.id)
                                bookmarkViewModel.toggleBookmark(article.id)
                                onBookmarksUpdated()
                                
                                // Track bookmark for ML (only when adding bookmark)
                                if (!wasBookmarked) {
                                    searchViewModel.onArticleBookmarked(article)
                                }
                                
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        if (!wasBookmarked)
                                            R.string.bookmark_added
                                        else
                                            R.string.bookmark_removed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onArticleClick = { article ->
                            // Track click for ML
                            searchViewModel.onArticleClicked(article)
                            onOpenArticle(article.id)
                        }
                    )
                    HomeTab.Bookmarks -> BookmarksTab(
                        modifier = Modifier.padding(innerPadding),
                        bookmarkViewModel = bookmarkViewModel,
                        bookmarks = bookmarkedArticles,
                        onArticleClick = { article ->
                            // Track click for ML
                            searchViewModel.onArticleClicked(article)
                            onOpenArticle(article.id)
                        }
                    )
                    HomeTab.Profile -> ProfileTab(
                        modifier = Modifier.padding(innerPadding),
                        profileViewModel = profileViewModel,
                        authState = authState,
                        onOpenSettings = onOpenSettings,
                        onRequireAuthentication = onRequireAuthentication
                    )
                }
            }
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
                    HomeTab.News -> "Today's News"
                    HomeTab.Search -> "Search News"
                    HomeTab.Bookmarks -> "Saved Articles"
                    HomeTab.Profile -> profile?.fullName().takeUnless { it.isNullOrBlank() } ?: "Profile"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            if (tab == HomeTab.Profile) {
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsTab(
    modifier: Modifier,
    newsViewModel: NewsViewModel,
    newsData: NewsUiState.Success,
    categoryArticles: List<NewsArticle>,
    personalizedFeatured: List<NewsArticle>,
    personalizedArticles: List<NewsArticle>,
    selectedCategory: String,
    isFetchingCategory: Boolean,
    isRefreshing: Boolean,
    bookmarkedIds: Set<Int>,
    onBookmarkToggle: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing || isFetchingCategory,
        onRefresh = { newsViewModel.refreshNews() },
        modifier = modifier
    ) {
        TransparentBackground(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Featured articles - filter by selected category
                val featuredForCategory = if (selectedCategory.equals("Top", ignoreCase = true)) {
                    // Show personalized featured for "Top"
                    if (personalizedFeatured.isNotEmpty()) {
                        personalizedFeatured
                    } else {
                        newsData.featuredArticles
                    }
                } else {
                    // Filter featured by selected category
                    categoryArticles.filter { it.isFeatured }
                }
                
                if (featuredForCategory.isNotEmpty()) {
                    item { SectionTitle("Featured") }
                    item {
                        FeaturedCarousel(
                            articles = featuredForCategory,
                            onArticleClick = onArticleClick
                        )
                    }
                }

                // Category tabs
                if (newsData.categories.isNotEmpty()) {
                    item {
                        CategoryRow(
                            categories = newsData.categories,
                            selected = selectedCategory,
                            onSelect = { newsViewModel.selectCategory(it.name) }
                        )
                    }
                }

                item { SectionTitle("For You") }

                // Articles list - use personalized for "Top", categoryArticles for specific categories
                val articlesToShow = if (selectedCategory.equals("Top", ignoreCase = true) && personalizedArticles.isNotEmpty()) {
                    personalizedArticles
                } else {
                    categoryArticles
                }
                
                items(articlesToShow) { article ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTab(
    modifier: Modifier,
    searchViewModel: SearchViewModel,
    searchQuery: String,
    searchResults: List<NewsArticle>,
    allArticles: List<NewsArticle>,
    bookmarkedIds: Set<Int>,
    onBookmarkToggle: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    val trimmedQuery = searchQuery.trim()
    val results = searchResults

    // Get ML-powered recommendations from ViewModel
    val recommendedArticles by searchViewModel.recommendedArticles.collectAsState()
    val isLoadingRecommendations by searchViewModel.isLoadingRecommendations.collectAsState()
    
    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    // Fallback recommendations if ML not ready
    val fallbackRecommendations = remember(allArticles) {
        allArticles.filter { it.category.equals("Sports", ignoreCase = true) }.ifEmpty {
            allArticles.take(5)
        }
    }

    // Use ML recommendations or fallback
    val displayRecommendations = if (recommendedArticles.isNotEmpty()) {
        recommendedArticles
    } else {
        fallbackRecommendations
    }

    TransparentBackground(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = isRefreshing || isLoadingRecommendations,
            onRefresh = {
                isRefreshing = true
                searchViewModel.refreshRecommendations()
                // Reset after a delay
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            value = searchQuery,
            onValueChange = { searchViewModel.onSearchQueryChange(it) },
            suggestions = emptyList()
        )

        AnimatedVisibility(
            visible = trimmedQuery.isBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ML Recommendations Section
                SectionTitle("✨ Recommended for You")
                // Loading state
                if (isLoadingRecommendations) {
                    repeat(3) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                } else if (displayRecommendations.isEmpty()) {
                    // Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🤖",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start reading to get personalized recommendations!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Display recommendations
                    displayRecommendations.forEach { article ->
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

        AnimatedVisibility(
            visible = trimmedQuery.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Results for \"$trimmedQuery\"")
                if (results.isEmpty()) {
                    Text(
                        text = "No matching articles found.",
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
}

@Composable
private fun BookmarksTab(
    modifier: Modifier,
    bookmarkViewModel: BookmarkViewModel,
    bookmarks: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit
) {
    TransparentBackground(modifier = modifier) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved articles yet.",
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
                onBookmarkToggle = { bookmarkViewModel.toggleBookmark(article.id) },
                onClick = { onArticleClick(article) }
            )
        }
        }
    }
}

@Composable
private fun ProfileTab(
    modifier: Modifier,
    profileViewModel: ProfileViewModel,
    authState: AuthState,
    onOpenSettings: () -> Unit,
    onRequireAuthentication: () -> Unit
) {
    TransparentBackground(modifier = modifier) {
        when (authState) {
            is AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@TransparentBackground
            }
            is AuthState.Guest -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Sign in to access your profile.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onRequireAuthentication) {
                            Text("Sign In")
                        }
                    }
                }
                return@TransparentBackground
            }
            is AuthState.Authenticated -> {
                val profile = authState.profile

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
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Manage preferences, edit profile, or sign out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
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
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundBitmap = rememberBackgroundBitmap(context)
    val alphaLevel = UserPreferences.getBackgroundAlphaLevel(context)
    val imageAlpha = imageAlphaForLevel(alphaLevel)
    
    Box(modifier = modifier.fillMaxSize()) {
        if (backgroundBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha),
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

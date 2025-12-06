package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.ml.ML_RuleBasedEngine
import com.example.newsapp.ml.ML_UserPreferenceTracker
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for News tab
 * Manages news data, categories, featured articles with StateFlow
 * Integrates ML recommendations for personalized content
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPreferenceTracker: ML_UserPreferenceTracker,
    private val ruleBasedEngine: ML_RuleBasedEngine
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    // Selected category
    private val _selectedCategory = MutableStateFlow("Top")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Category-specific articles
    private val _categoryArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val categoryArticles: StateFlow<List<NewsArticle>> = _categoryArticles.asStateFlow()

    // Loading state for category switch
    private val _isFetchingCategory = MutableStateFlow(false)
    val isFetchingCategory: StateFlow<Boolean> = _isFetchingCategory.asStateFlow()

    // Refresh state for pull-to-refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Personalized featured articles (sorted by preference)
    private val _personalizedFeatured = MutableStateFlow<List<NewsArticle>>(emptyList())
    val personalizedFeatured: StateFlow<List<NewsArticle>> = _personalizedFeatured.asStateFlow()

    // Personalized articles for "For You" section
    private val _personalizedArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val personalizedArticles: StateFlow<List<NewsArticle>> = _personalizedArticles.asStateFlow()

    init {
        loadNews()
    }

    /**
     * Load initial news data
     */
    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            try {
                val newsData = newsRepository.getNewsData()
                _uiState.value = NewsUiState.Success(
                    articles = newsData.articles,
                    featuredArticles = newsData.featuredArticles,
                    categories = newsData.categories
                )
                _categoryArticles.value = newsData.articles
                
                // Generate personalized content
                applyPersonalization(newsData.articles, newsData.featuredArticles)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh news data (pull-to-refresh)
     */
    fun refreshNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                newsRepository.invalidateCache()
                val newsData = newsRepository.getNewsData()
                _uiState.value = NewsUiState.Success(
                    articles = newsData.articles,
                    featuredArticles = newsData.featuredArticles,
                    categories = newsData.categories
                )
                // Refresh current category articles
                if (_selectedCategory.value.equals("Top", ignoreCase = true)) {
                    _categoryArticles.value = newsData.articles
                    applyPersonalization(newsData.articles, newsData.featuredArticles)
                } else {
                    selectCategory(_selectedCategory.value)
                }
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Select category and fetch articles for that category
     */
    fun selectCategory(category: String) {
        _selectedCategory.value = category
        
        viewModelScope.launch {
            if (category.equals("Top", ignoreCase = true)) {
                // Use cached articles for "Top" - apply personalization
                val currentState = _uiState.value
                if (currentState is NewsUiState.Success) {
                    _categoryArticles.value = currentState.articles
                    applyPersonalization(currentState.articles, currentState.featuredArticles)
                }
            } else {
                // Fetch category-specific articles
                _isFetchingCategory.value = true
                try {
                    val result = newsRepository.fetchArticlesFromNetwork(
                        category = category,
                        country = "us"
                    )
                    if (result is Resource.Success) {
                        _categoryArticles.value = result.data
                        
                        // Apply personalization to category-specific featured
                        val currentState = _uiState.value
                        if (currentState is NewsUiState.Success) {
                            applyPersonalization(result.data, currentState.featuredArticles)
                        }
                    } else if (result is Resource.Error) {
                        val isRateLimited = result.message?.contains("429") == true ||
                                result.message?.contains("rate limit", ignoreCase = true) == true
                        if (isRateLimited) {
                            val cached = newsRepository.getCachedArticlesByCategory(category)
                            if (cached.isNotEmpty()) {
                                _categoryArticles.value = cached
                                val currentState = _uiState.value
                                if (currentState is NewsUiState.Success) {
                                    applyPersonalization(cached, currentState.featuredArticles)
                                    _uiState.value = currentState // keep success state
                                }
                                return@launch
                            }
                        }
                        // Keep existing articles if fetch fails and no cache
                        _uiState.value = NewsUiState.Error(result.message ?: "Failed to load category")
                    }
                } catch (e: Exception) {
                    _uiState.value = NewsUiState.Error(e.message ?: "Failed to load category")
                } finally {
                    _isFetchingCategory.value = false
                }
            }
        }
    }

    /**
     * Apply ML-based personalization to articles and featured content
     */
    private fun applyPersonalization(
        articles: List<NewsArticle>,
        featuredArticles: List<NewsArticle>
    ) {
        viewModelScope.launch {
            val preferences = userPreferenceTracker.categoryScores.value
            
            if (preferences.isEmpty()) {
                // No preferences yet (balanced init) - shuffle for random order
                _personalizedFeatured.value = featuredArticles.shuffled()
                _personalizedArticles.value = articles.shuffled()
                android.util.Log.d("NewsViewModel", "🎲 Balanced preferences - articles shuffled randomly")
                return@launch
            }
            
            // Group featured by category, shuffle within groups, then sort by preference
            val sortedFeatured = featuredArticles
                .groupBy { it.category }
                .flatMap { (_, articlesInCategory) -> articlesInCategory.shuffled() }
                .sortedByDescending { article -> preferences.getOrDefault(article.category, 0f) }
            
            // Get personalized recommendations for "For You"
            val recommendations = ruleBasedEngine.getRecommendations(
                articles = articles,
                count = minOf(articles.size, 50) // Top 50 personalized articles
            )
            
            _personalizedFeatured.value = sortedFeatured
            _personalizedArticles.value = recommendations.map { it.first }
            
            android.util.Log.d("NewsViewModel", "📊 Personalization applied: ${preferences.size} categories tracked")
            android.util.Log.d("NewsViewModel", "🎯 Top preference: ${preferences.maxByOrNull { it.value }?.key}")
        }
    }

    /**
     * Get bookmarked article IDs (suspend function for Firestore access)
     */
    suspend fun getBookmarkedIds(): Set<Int> {
        return newsRepository.getBookmarks().map { it.id }.toSet()
    }
}

/**
 * UI State sealed class for News screen
 */
sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(
        val articles: List<NewsArticle>,
        val featuredArticles: List<NewsArticle>,
        val categories: List<NewsCategory>
    ) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

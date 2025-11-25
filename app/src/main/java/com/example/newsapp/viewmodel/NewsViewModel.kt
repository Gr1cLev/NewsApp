package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
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
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    // Selected category
    private val _selectedCategory = MutableStateFlow("All")
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
                if (_selectedCategory.value.equals("All", ignoreCase = true)) {
                    _categoryArticles.value = newsData.articles
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
            if (category.equals("All", ignoreCase = true)) {
                // Use cached articles for "All"
                val currentState = _uiState.value
                if (currentState is NewsUiState.Success) {
                    _categoryArticles.value = currentState.articles
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
                    } else if (result is Resource.Error) {
                        // Keep existing articles if fetch fails
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
     * Get bookmarked article IDs
     */
    fun getBookmarkedIds(): Set<Int> {
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

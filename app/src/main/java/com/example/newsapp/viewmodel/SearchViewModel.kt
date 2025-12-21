package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.ml.ML_RecommendationEngine
import com.example.newsapp.ml.ML_ScoringWeights
import com.example.newsapp.ml.ML_UserPreferenceTracker
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search tab
 * Manages search query, results, suggestions, and ML-powered recommendations
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val recommendationEngine: ML_RecommendationEngine,
    private val userPreferenceTracker: ML_UserPreferenceTracker,
    private val authRepository: FirebaseAuthRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Search results
    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults.asStateFlow()

    // Search suggestions
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // Loading state
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ML Recommendations state
    private val _recommendedArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val recommendedArticles: StateFlow<List<NewsArticle>> = _recommendedArticles.asStateFlow()

    // Recommendation loading state
    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    // Model status
    private val _modelStatus = MutableStateFlow<String>("Initializing...")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()

    // Debounce job for API search
    private var searchJob: Job? = null
    private var recommendationJob: Job? = null

    // All articles for local filtering (fallback)
    private val _allArticles = MutableStateFlow<List<NewsArticle>>(emptyList())

    init {
        loadSuggestions()
        loadAllArticles()
        // Initialize ML engine (downloads model from Firestore if needed)
        viewModelScope.launch {
            recommendationEngine.initialize()
        }
        loadRecommendations()
        observeModelStatus()
    }

    /**
     * Load search suggestions
     */
    private fun loadSuggestions() {
        viewModelScope.launch {
            _suggestions.value = newsRepository.getSearchSuggestions()
        }
    }

    /**
     * Load all articles for local search
     */
    private fun loadAllArticles() {
        viewModelScope.launch {
            _allArticles.value = newsRepository.getArticles()
        }
    }

    /**
     * Update search query and perform search
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
        
        // Cancel previous search job
        searchJob?.cancel()
        
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        // Debounce: wait 500ms before calling API
        searchJob = viewModelScope.launch {
            delay(500)
            performApiSearch(trimmedQuery)
        }
    }

    /**
     * Perform search via API
     */
    private suspend fun performApiSearch(query: String) {
        _isSearching.value = true
        try {
            val result = newsRepository.searchArticles(query)
            when (result) {
                is Resource.Success -> {
                    _searchResults.value = result.data
                    if (result.data.isEmpty()) {
                        // Fallback to local search if API returns empty
                        performLocalSearch(query)
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                    // Fallback to local search on error
                    performLocalSearch(query)
                }
                is Resource.Loading -> {
                    // Do nothing
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            performLocalSearch(query)
        } finally {
            _isSearching.value = false
        }
    }

    /**
     * Perform local search (fallback)
     */
    private fun performLocalSearch(query: String) {
        val results = _allArticles.value.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.summary.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
        if (results.isNotEmpty()) {
            _searchResults.value = results
        }
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    /**
     * Get bookmarked article IDs (suspend function for Firestore access)
     */
    suspend fun getBookmarkedIds(): Set<Int> {
        return newsRepository.getBookmarks().map { it.id }.toSet()
    }

    /**
     * Refresh articles for search
     */
    fun refreshArticles() {
        loadAllArticles()
        loadSuggestions()
        loadRecommendations()
    }

    /**
     * Load personalized recommendations using ML engine
     */
    fun loadRecommendations() {
        // Cancel previous job
        recommendationJob?.cancel()
        
        recommendationJob = viewModelScope.launch {
            _isLoadingRecommendations.value = true
            
            try {
                // Get current user ID
                val userId = authRepository.getCurrentUser()?.uid ?: "anonymous"
                
                // Always refresh articles from repository
                val candidates = newsRepository.getArticles()
                _allArticles.value = candidates
                
                if (candidates.isEmpty()) {
                    _recommendedArticles.value = emptyList()
                    return@launch
                }
                
                // Get current preference scores
                val totalInteractions = userPreferenceTracker.getTotalInteractions()
                android.util.Log.d(
                    "SearchViewModel",
                    "📊 User interactions: $totalInteractions"
                )
                
                // Log current preferences
                val preferences = userPreferenceTracker.categoryScores.value
                android.util.Log.d(
                    "SearchViewModel",
                    "🎯 Current preferences: $preferences"
                )
                
                // Choose scoring weights based on user preference data
                val weights = if (totalInteractions < 70) {
                    // New user: prefer rule-based (cold start)
                    android.util.Log.d("SearchViewModel", "⚙️ Using RULE_HEAVY weights (cold start)")
                    ML_ScoringWeights.RULE_HEAVY
                } else {
                    // Experienced user: balanced approach
                    android.util.Log.d("SearchViewModel", "⚙️ Using DEFAULT weights")
                    ML_ScoringWeights.DEFAULT
                }
                
                // Get recommendations from ML engine
                val scoredArticles = recommendationEngine.getRecommendations(
                    userId = userId,
                    candidates = candidates,
                    topN = 10,
                    weights = weights
                )
                
                // Log top recommendations with scores
                scoredArticles.take(3).forEach { scored ->
                    android.util.Log.d(
                        "SearchViewModel",
                        "  📰 [${scored.finalScore}] ${scored.article.category}: ${scored.article.title}"
                    )
                }
                
                // Log category distribution
                val categoryDistribution = scoredArticles.groupBy { it.article.category }
                    .mapValues { it.value.size }
                android.util.Log.d(
                    "SearchViewModel",
                    "  📊 Category distribution: $categoryDistribution"
                )
                
                // Extract articles
                _recommendedArticles.value = scoredArticles.map { it.article }
                
                android.util.Log.d(
                    "SearchViewModel",
                    "✅ Loaded ${scoredArticles.size} recommendations for user=$userId"
                )
                
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Failed to load recommendations", e)
                _recommendedArticles.value = emptyList()
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    /**
     * Observe ML model status
     */
    private fun observeModelStatus() {
        viewModelScope.launch {
            recommendationEngine.modelStatus.collect { status ->
                _modelStatus.value = when (status) {
                    is ML_RecommendationEngine.ModelStatus.NotLoaded -> ""
                    is ML_RecommendationEngine.ModelStatus.Downloading -> "Downloading ML model..."
                    is ML_RecommendationEngine.ModelStatus.Loaded -> "ML model ready (v${status.version})"
                    is ML_RecommendationEngine.ModelStatus.Error -> "ML model error: ${status.message}"
                }
            }
        }
    }

    /**
     * Track article click for ML learning
     */
    fun onArticleClicked(article: NewsArticle) {
        viewModelScope.launch {
            android.util.Log.d(
                "SearchViewModel",
                "👆 Article clicked: ${article.title} (${article.category})"
            )
            userPreferenceTracker.onArticleClicked(article)
            // Reload recommendations after interaction
            loadRecommendations()
        }
    }

    /**
     * Track article bookmark for ML learning (only tracks when adding bookmark)
     */
    fun onArticleBookmarked(article: NewsArticle) {
        viewModelScope.launch {
            android.util.Log.d(
                "SearchViewModel",
                "🔖 Article bookmarked: ${article.title} (${article.category})"
            )
            userPreferenceTracker.onArticleBookmarked(article, isBookmarked = true)
            // Reload recommendations after interaction
            loadRecommendations()
        }
    }

    /**
     * Force refresh ML recommendations
     */
    fun refreshRecommendations() {
        loadRecommendations()
    }
}

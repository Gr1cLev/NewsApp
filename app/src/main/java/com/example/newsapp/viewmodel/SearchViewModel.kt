package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
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
 * Manages search query, results, and suggestions
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val newsRepository: NewsRepository
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

    // Debounce job for API search
    private var searchJob: Job? = null

    // All articles for local filtering (fallback)
    private val _allArticles = MutableStateFlow<List<NewsArticle>>(emptyList())

    init {
        loadSuggestions()
        loadAllArticles()
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
     * Get bookmarked article IDs
     */
    fun getBookmarkedIds(): Set<Int> {
        return newsRepository.getBookmarks().map { it.id }.toSet()
    }

    /**
     * Refresh articles for search
     */
    fun refreshArticles() {
        loadAllArticles()
        loadSuggestions()
    }
}

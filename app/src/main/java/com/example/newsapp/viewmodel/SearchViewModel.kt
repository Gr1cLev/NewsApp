package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.model.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // All articles for local filtering
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
        performSearch(query)
    }

    /**
     * Perform search (local filtering)
     */
    private fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = _allArticles.value.filter {
                    it.title.contains(trimmedQuery, ignoreCase = true) ||
                    it.summary.contains(trimmedQuery, ignoreCase = true) ||
                    it.category.contains(trimmedQuery, ignoreCase = true)
                }
                _searchResults.value = results
            } finally {
                _isSearching.value = false
            }
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

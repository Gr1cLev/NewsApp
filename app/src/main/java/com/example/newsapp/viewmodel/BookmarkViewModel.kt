package com.example.newsapp.viewmodel

import android.widget.Toast
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
 * ViewModel for Bookmarks
 * Manages bookmarked articles and toggle operations
 */
@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    // Bookmarked articles
    private val _bookmarkedArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val bookmarkedArticles: StateFlow<List<NewsArticle>> = _bookmarkedArticles.asStateFlow()

    // Bookmarked IDs for quick lookup
    private val _bookmarkedIds = MutableStateFlow<Set<Int>>(emptySet())
    val bookmarkedIds: StateFlow<Set<Int>> = _bookmarkedIds.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBookmarks()
    }

    /**
     * Load bookmarked articles
     */
    fun loadBookmarks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bookmarks = newsRepository.getBookmarks()
                _bookmarkedArticles.value = bookmarks
                _bookmarkedIds.value = bookmarks.map { it.id }.toSet()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle bookmark for an article
     * Returns true if bookmarked, false if removed
     */
    fun toggleBookmark(articleId: Int): Boolean {
        var isBookmarked = false
        viewModelScope.launch {
            isBookmarked = newsRepository.toggleBookmark(articleId)
            // Reload bookmarks to update UI
            loadBookmarks()
        }
        return isBookmarked
    }

    /**
     * Check if article is bookmarked
     */
    fun isArticleBookmarked(articleId: Int): Boolean {
        return _bookmarkedIds.value.contains(articleId)
    }

    /**
     * Refresh bookmarks
     */
    fun refresh() {
        loadBookmarks()
    }
}

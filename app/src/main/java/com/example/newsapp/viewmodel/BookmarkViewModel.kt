package com.example.newsapp.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.data.firebase.UserInteractionRepository
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
    private val newsRepository: NewsRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val userInteractionRepository: UserInteractionRepository
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
            // Toggle bookmark in repository (now async)
            isBookmarked = newsRepository.toggleBookmark(articleId)
            
            // Track bookmark action in Firebase with full article info
            val userId = firebaseAuthRepository.getCurrentUserId()
            if (userId != null) {
                android.util.Log.d("BookmarkViewModel", "Tracking bookmark for article $articleId, isBookmarked=$isBookmarked")
                
                // Get article info for analytics
                val article = newsRepository.getArticleById(articleId)
                if (article != null) {
                    userInteractionRepository.trackBookmark(
                        articleId = articleId.toString(),
                        isBookmarked = isBookmarked,
                        title = article.title,
                        category = article.category
                    )
                    android.util.Log.d("BookmarkViewModel", "Bookmark tracked with article info")
                } else {
                    android.util.Log.w("BookmarkViewModel", "Article not found in repository for ID: $articleId")
                }
            } else {
                android.util.Log.w("BookmarkViewModel", "User not authenticated, skipping Firebase tracking")
            }
            
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

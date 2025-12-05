package com.example.newsapp.ml

import android.content.Context
import android.content.SharedPreferences
import com.example.newsapp.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Tracks user preferences in real-time for instant personalization
 * 
 * This component updates immediately when user interacts with articles,
 * providing instant response without waiting for ML model retraining.
 * 
 * Tracked metrics:
 * - Category preferences (which categories user likes)
 * - Reading patterns (time spent per category)
 * - Bookmark behavior
 * - Recent article history
 */
@Singleton
class ML_UserPreferenceTracker @Inject constructor(
    private val context: Context
) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Category scores (higher = more interested)
    private val _categoryScores = MutableStateFlow<Map<String, Float>>(loadCategoryScores())
    val categoryScores: StateFlow<Map<String, Float>> = _categoryScores.asStateFlow()
    
    // Recent articles (for recency boost)
    private val _recentArticles = MutableStateFlow<List<String>>(loadRecentArticles())
    val recentArticles: StateFlow<List<String>> = _recentArticles.asStateFlow()
    
    // Total interactions count
    private val _totalInteractions = MutableStateFlow(loadTotalInteractions())
    val totalInteractions: StateFlow<Int> = _totalInteractions.asStateFlow()
    
    /**
     * Track article click - increment category preference
     */
    fun onArticleClicked(article: NewsArticle) {
        val currentScores = _categoryScores.value.toMutableMap()
        val category = article.category
        
        // Increment category score
        currentScores[category] = (currentScores[category] ?: 0f) + CLICK_WEIGHT
        
        // Update recent articles
        val recentList = _recentArticles.value.toMutableList()
        recentList.add(0, article.id.toString())
        if (recentList.size > MAX_RECENT_ARTICLES) {
            recentList.removeLast()
        }
        
        // Increment total interactions
        _totalInteractions.value += 1
        
        // Update state
        _categoryScores.value = currentScores
        _recentArticles.value = recentList
        
        // Persist to disk
        saveCategoryScores(currentScores)
        saveRecentArticles(recentList)
        saveTotalInteractions(_totalInteractions.value)
        
        android.util.Log.d(TAG, "Article clicked: ${article.title}, Category: $category, New score: ${currentScores[category]}")
    }
    
    /**
     * Track reading time - boost category preference based on engagement
     */
    fun onReadingTimeTracked(article: NewsArticle, durationSeconds: Long) {
        if (durationSeconds < MIN_READING_TIME_SECONDS) return
        
        val currentScores = _categoryScores.value.toMutableMap()
        val category = article.category
        
        // Boost based on reading time (1 minute = 1 point)
        val timeBoost = min(durationSeconds / 60f, MAX_TIME_BOOST)
        currentScores[category] = (currentScores[category] ?: 0f) + timeBoost
        
        _categoryScores.value = currentScores
        saveCategoryScores(currentScores)
        
        android.util.Log.d(TAG, "Reading time tracked: ${article.title}, Duration: ${durationSeconds}s, Time boost: $timeBoost")
    }
    
    /**
     * Track bookmark - strong positive signal
     */
    fun onArticleBookmarked(article: NewsArticle, isBookmarked: Boolean) {
        val currentScores = _categoryScores.value.toMutableMap()
        val category = article.category
        
        val boost = if (isBookmarked) BOOKMARK_WEIGHT else -BOOKMARK_WEIGHT
        currentScores[category] = (currentScores[category] ?: 0f) + boost
        
        _categoryScores.value = currentScores
        saveCategoryScores(currentScores)
        
        android.util.Log.d(TAG, "Article bookmarked: ${article.title}, Category: $category, Bookmarked: $isBookmarked")
    }
    
    /**
     * Get preference boost for an article based on real-time tracking
     * Returns normalized score 0.0 - 1.0
     */
    fun getPreferenceBoost(article: NewsArticle): Float {
        val categoryScore = _categoryScores.value[article.category] ?: 0f
        val isRecent = _recentArticles.value.contains(article.id.toString())
        
        // Normalize category score (max 10 points)
        val normalizedCategoryScore = min(categoryScore / 10f, 1.0f)
        
        // Recency boost (articles from recently clicked categories)
        val recencyBoost = if (isRecent) 0.3f else 0f
        
        // Combine (max 1.0)
        return min(normalizedCategoryScore + recencyBoost, 1.0f)
    }
    
    /**
     * Get user's favorite categories (top 3)
     */
    fun getFavoriteCategories(): List<String> {
        return _categoryScores.value
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }
    
    /**
     * Check if user has enough data for ML recommendations
     */
    fun hasEnoughDataForML(): Boolean {
        return _totalInteractions.value >= MIN_INTERACTIONS_FOR_ML
    }
    
    /**
     * Reset all preferences (for testing or user request)
     */
    fun reset() {
        _categoryScores.value = emptyMap()
        _recentArticles.value = emptyList()
        _totalInteractions.value = 0
        
        prefs.edit()
            .clear()
            .apply()
        
        android.util.Log.d(TAG, "User preferences reset")
    }
    
    // ===== Persistence Methods =====
    
    private fun loadCategoryScores(): Map<String, Float> {
        val scoresString = prefs.getString(KEY_CATEGORY_SCORES, null) ?: return emptyMap()
        return try {
            scoresString.split(";")
                .mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1].toFloat() else null
                }
                .toMap()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading category scores", e)
            emptyMap()
        }
    }
    
    private fun saveCategoryScores(scores: Map<String, Float>) {
        val scoresString = scores.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit()
            .putString(KEY_CATEGORY_SCORES, scoresString)
            .apply()
    }
    
    private fun loadRecentArticles(): List<String> {
        val articlesString = prefs.getString(KEY_RECENT_ARTICLES, null) ?: return emptyList()
        return articlesString.split(",").filter { it.isNotEmpty() }
    }
    
    private fun saveRecentArticles(articles: List<String>) {
        val articlesString = articles.joinToString(",")
        prefs.edit()
            .putString(KEY_RECENT_ARTICLES, articlesString)
            .apply()
    }
    
    private fun loadTotalInteractions(): Int {
        return prefs.getInt(KEY_TOTAL_INTERACTIONS, 0)
    }
    
    private fun saveTotalInteractions(count: Int) {
        prefs.edit()
            .putInt(KEY_TOTAL_INTERACTIONS, count)
            .apply()
    }
    
    companion object {
        private const val TAG = "ML_UserPreferenceTracker"
        private const val PREFS_NAME = "ml_user_preferences"
        
        // Keys
        private const val KEY_CATEGORY_SCORES = "category_scores"
        private const val KEY_RECENT_ARTICLES = "recent_articles"
        private const val KEY_TOTAL_INTERACTIONS = "total_interactions"
        
        // Weights
        private const val CLICK_WEIGHT = 1.0f
        private const val BOOKMARK_WEIGHT = 3.0f
        private const val MAX_TIME_BOOST = 3.0f
        
        // Thresholds
        private const val MIN_READING_TIME_SECONDS = 5L
        private const val MAX_RECENT_ARTICLES = 20
        private const val MIN_INTERACTIONS_FOR_ML = 10
    }
}

package com.example.newsapp.ml

import android.content.Context
import android.content.SharedPreferences
import com.example.newsapp.model.NewsArticle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Tracks user preferences in real-time for instant personalization
 * 
 * This component updates immediately when user interacts with articles,
 * providing instant response without waiting for ML model retraining.
 * 
 * Features:
 * - Per-user preference isolation (userId prefix)
 * - Firestore sync for cross-device persistence
 * - Reactive to auth state changes
 * 
 * Tracked metrics:
 * - Category preferences (which categories user likes)
 * - Reading patterns (time spent per category)
 * - Bookmark behavior
 * - Recent article history
 */
@Singleton
class ML_UserPreferenceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    @Volatile
    private var currentUserId: String? = null
    
    private val prefs: SharedPreferences
        get() {
            val userId = currentUserId ?: firebaseAuth.currentUser?.uid ?: "guest"
            return context.getSharedPreferences("${PREFS_NAME}_$userId", Context.MODE_PRIVATE)
        }
    
    // Category scores (higher = more interested) - MUST be initialized BEFORE init block!
    private val _categoryScores = MutableStateFlow<Map<String, Float>>(emptyMap())
    val categoryScores: StateFlow<Map<String, Float>> = _categoryScores.asStateFlow()
    
    // Recent articles (for recency boost)
    private val _recentArticles = MutableStateFlow<List<String>>(emptyList())
    val recentArticles: StateFlow<List<String>> = _recentArticles.asStateFlow()
    
    // Total interactions count
    private val _totalInteractions = MutableStateFlow(0)
    val totalInteractions: StateFlow<Int> = _totalInteractions.asStateFlow()
    
    init {
        // Listen to auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            val newUserId = auth.currentUser?.uid
            if (newUserId != currentUserId) {
                android.util.Log.d(TAG, "Auth state changed: $currentUserId -> $newUserId")
                
                // IMPORTANT: Clear StateFlows immediately to prevent showing old user's data
                _categoryScores.value = emptyMap()
                _recentArticles.value = emptyList()
                _totalInteractions.value = 0
                
                currentUserId = newUserId
                
                // Reload preferences for new user
                coroutineScope.launch {
                    loadPreferencesForCurrentUser()
                }
            }
        }
        
        // Initial load - load preferences for current user
        coroutineScope.launch {
            currentUserId = firebaseAuth.currentUser?.uid
            loadPreferencesForCurrentUser()
        }
    }
    
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
        
        // Sync to Firestore (non-blocking)
        syncToFirestore()
        
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
        
        // Sync to Firestore
        syncToFirestore()
        
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
     * Get total number of interactions (for determining user maturity)
     */
    fun getTotalInteractions(): Int {
        return _totalInteractions.value
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
    
    /**
     * Load preferences for current user from Firestore (with local fallback)
     */
    private suspend fun loadPreferencesForCurrentUser() {
        try {
            val userId = currentUserId
            if (userId == null) {
                android.util.Log.w(TAG, "No user logged in, using empty preferences")
                clearPreferences()
                return
            }
            
            android.util.Log.d(TAG, "Loading preferences for user: $userId")
            
            // Try Firestore first
            val firestoreData = try {
                firestore.collection("user_preferences")
                    .document(userId)
                    .collection("ml_data")
                    .document("preferences")
                    .get()
                    .await()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to load from Firestore: ${e.message}")
                null
            }
            
            if (firestoreData != null && firestoreData.exists()) {
                // Load from Firestore
                val categoryScoresMap = firestoreData.get("categoryScores") as? Map<String, Any>
                val categoryScores = categoryScoresMap?.mapValues { (it.value as? Number)?.toFloat() ?: 0f } ?: emptyMap()
                val totalInteractions = (firestoreData.get("totalInteractions") as? Number)?.toInt() ?: 0
                
                _categoryScores.value = categoryScores
                _totalInteractions.value = totalInteractions
                _recentArticles.value = emptyList()
                
                // Save to local cache for this userId
                saveCategoryScores(categoryScores)
                saveTotalInteractions(totalInteractions)
                saveRecentArticles(emptyList())
                
                android.util.Log.d(TAG, "✅ Loaded preferences from Firestore: ${categoryScores.size} categories, $totalInteractions interactions")
            } else {
                // No Firestore data - load from local SharedPreferences (per-userId)
                val localScores = loadCategoryScores()
                val localInteractions = loadTotalInteractions()
                val localRecent = loadRecentArticles()
                
                _categoryScores.value = localScores
                _totalInteractions.value = localInteractions
                _recentArticles.value = localRecent
                
                if (localScores.isEmpty()) {
                    android.util.Log.d(TAG, "🆕 New user - starting with empty preferences")
                } else {
                    android.util.Log.d(TAG, "📱 Loaded preferences from local storage: ${localScores.size} categories")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading preferences: ${e.message}", e)
            clearPreferences()
        }
    }
    
    /**
     * Sync current preferences to Firestore
     */
    private fun syncToFirestore() {
        val userId = currentUserId ?: return
        
        coroutineScope.launch {
            try {
                val data = hashMapOf(
                    "categoryScores" to _categoryScores.value,
                    "totalInteractions" to _totalInteractions.value,
                    "lastUpdated" to com.google.firebase.Timestamp.now(),
                    "version" to 1
                )
                
                firestore.collection("user_preferences")
                    .document(userId)
                    .collection("ml_data")
                    .document("preferences")
                    .set(data)
                    .await()
                
                android.util.Log.d(TAG, "☁️ Synced preferences to Firestore for user: $userId")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to sync to Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear all preferences (on logout)
     */
    fun clearPreferences() {
        _categoryScores.value = emptyMap()
        _recentArticles.value = emptyList()
        _totalInteractions.value = 0
        
        // Clear local storage
        prefs.edit().clear().apply()
        
        android.util.Log.d(TAG, "🗑️ Cleared all preferences")
    }
    
    /**
     * Initialize balanced preferences for new user
     * All categories start with equal weight (5.0) for fair recommendations
     */
    suspend fun initializeBalancedPreferences(userId: String) {
        try {
            // Check if preferences already exist
            val existingData = firestore.collection("user_preferences")
                .document(userId)
                .collection("ml_data")
                .document("preferences")
                .get()
                .await()
            
            if (existingData.exists()) {
                android.util.Log.d(TAG, "⚠️ Preferences already exist for user: $userId")
                return
            }
            
            // Create balanced initial preferences
            val balancedScores = mapOf(
                "Sports" to 5.0f,
                "Business" to 5.0f,
                "Technology" to 5.0f,
                "Health" to 5.0f,
                "Entertainment" to 5.0f
            )
            
            val initialData = hashMapOf(
                "categoryScores" to balancedScores,
                "totalInteractions" to 0,
                "lastUpdated" to com.google.firebase.Timestamp.now(),
                "version" to 1
            )
            
            // Save to Firestore
            firestore.collection("user_preferences")
                .document(userId)
                .collection("ml_data")
                .document("preferences")
                .set(initialData)
                .await()
            
            // Update local StateFlows
            _categoryScores.value = balancedScores
            _totalInteractions.value = 0
            _recentArticles.value = emptyList()
            
            // Save to local cache
            saveCategoryScores(balancedScores)
            saveTotalInteractions(0)
            saveRecentArticles(emptyList())
            
            android.util.Log.d(TAG, "✅ Initialized balanced preferences for new user: $userId")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize preferences: ${e.message}", e)
        }
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
        private const val MIN_INTERACTIONS_FOR_ML = 70
        
        // Default categories for balanced initialization
        val DEFAULT_CATEGORIES = listOf(
            "Sports", "Business", "Technology", "Health", "Entertainment"
        )
        const val BALANCED_INITIAL_SCORE = 5.0f
    }
}

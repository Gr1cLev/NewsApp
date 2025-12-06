package com.example.newsapp.ml

import com.example.newsapp.model.NewsArticle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.min

/**
 * Rule-based recommendation engine for instant personalization
 * 
 * This engine provides real-time recommendations based on:
 * 1. Category preferences (from ML_UserPreferenceTracker)
 * 2. Recency (fresh articles get boost)
 * 3. Reading patterns
 * 
 * Unlike ML model which trains periodically, this updates instantly
 * when user interacts with articles.
 */
@Singleton
class ML_RuleBasedEngine @Inject constructor(
    private val preferenceTracker: ML_UserPreferenceTracker
) {
    
    /**
     * Calculate rule-based score for a single article
     * 
     * Simplified: Focus 100% on category preference
     * Recency and text-based boost moved to ML model
     * 
     * @return Normalized score 0.0 - 1.0
     */
    fun calculateScore(article: NewsArticle): Float {
        return getCategoryScore(article)
    }
    
    /**
     * Calculate scores for multiple articles
     * Optimized batch processing
     */
    fun calculateScores(articles: List<NewsArticle>): List<Float> {
        return articles.map { calculateScore(it) }
    }
    
    /**
     * Get recommended articles sorted by category preference
     * 
     * New logic: Prioritize articles from top categories, shuffle within category for freshness
     * 
     * @param articles Pool of articles to rank
     * @param count Number of recommendations to return
     * @return Top N articles from preferred categories (shuffled for variety)
     */
    fun getRecommendations(
        articles: List<NewsArticle>,
        count: Int = 10
    ): List<Pair<NewsArticle, Float>> {
        val categoryScores = preferenceTracker.categoryScores.value
        
        if (categoryScores.isEmpty()) {
            // No preference yet - return random articles with neutral score
            return articles.shuffled().take(count).map { it to 0.5f }
        }
        
        // Get top 3 favorite categories
        val topCategories = categoryScores.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        android.util.Log.d(
            "ML_RuleBasedEngine",
            "Top categories: $topCategories from preferences: $categoryScores"
        )
        
        // Prioritize articles from top categories
        val favoriteArticles = articles.filter { it.category in topCategories }.shuffled()
        val otherArticles = articles.filter { it.category !in topCategories }.shuffled()
        
        // Combine: 80% from favorites, 20% from others (for diversity)
        val favoriteCount = (count * 0.8).toInt()
        val recommended = favoriteArticles.take(favoriteCount) + 
                         otherArticles.take(count - favoriteCount)
        
        // Assign scores based on category preference
        return recommended.take(count).map { article ->
            val categoryScore = getCategoryScore(article)
            article to categoryScore
        }
    }
    
    /**
     * Calculate category preference score
     * Based on how much user interacts with this category
     */
    private fun getCategoryScore(article: NewsArticle): Float {
        val categoryScores = preferenceTracker.categoryScores.value
        val totalScore = categoryScores.values.sum()
        
        if (totalScore == 0f) {
            // No user data yet - return neutral score
            return 0.5f
        }
        
        val categoryScore = categoryScores[article.category] ?: 0f
        val maxScore = categoryScores.values.maxOrNull() ?: 1f
        
        // Normalize relative to max category (gives stronger differentiation)
        // If this is the top category, give it 1.0
        // Other categories scale proportionally
        return if (maxScore > 0f) {
            categoryScore / maxScore
        } else {
            0.5f
        }
    }
    
    /**
     * Calculate recency score
     * Fresh articles (< 24 hours) get maximum score
     * Older articles decay logarithmically
     * 
     * Note: publishedAt is a String in NewsArticle model
     * For now, return neutral score until proper timestamp parsing is implemented
     */
    private fun getRecencyScore(article: NewsArticle): Float {
        // TODO: Parse article.publishedAt string to timestamp
        // For now, return neutral score
        return 0.5f
    }
    
    /**
     * Filter articles by user preferences
     * Returns articles from favorite categories
     */
    fun filterByPreferences(articles: List<NewsArticle>): List<NewsArticle> {
        val favoriteCategories = preferenceTracker.getFavoriteCategories()
        
        if (favoriteCategories.isEmpty()) {
            // No preferences yet, return all
            return articles
        }
        
        return articles.filter { article ->
            favoriteCategories.contains(article.category)
        }
    }
    
    /**
     * Check if user has enough interaction data for meaningful recommendations
     */
    fun hasEnoughData(): Boolean {
        return preferenceTracker.totalInteractions.value >= MIN_INTERACTIONS_NEEDED
    }
    
    /**
     * Get trending score for an article
     * This can be extended to use global click counts from Firebase
     * For now, returns 0.5 (neutral)
     */
    fun getTrendingScore(article: NewsArticle): Float {
        // TODO: Implement global trending calculation from Firebase Analytics
        // For now, return neutral score
        return 0.5f
    }
    
    companion object {
        private const val TAG = "ML_RuleBasedEngine"
        private const val MIN_INTERACTIONS_NEEDED = 3
        
        private fun min(a: Float, b: Float) = if (a < b) a else b
        private fun max(a: Float, b: Float) = if (a > b) a else b
    }
}

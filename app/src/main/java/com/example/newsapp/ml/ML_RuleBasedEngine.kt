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
     * Score components:
     * - Category preference (0-1): Based on user's interaction history
     * - Recency score (0-1): How fresh the article is
     * - Bookmark affinity (0-1): If user bookmarks this category
     * 
     * @return Normalized score 0.0 - 1.0
     */
    fun calculateScore(article: NewsArticle): Float {
        val categoryScore = getCategoryScore(article)
        val recencyScore = getRecencyScore(article)
        val preferenceBoost = preferenceTracker.getPreferenceBoost(article)
        
        // Weighted combination
        val score = (categoryScore * 0.5f) +      // 50% category preference
                    (recencyScore * 0.3f) +        // 30% recency
                    (preferenceBoost * 0.2f)       // 20% user preference boost
        
        return min(score, 1.0f)
    }
    
    /**
     * Calculate scores for multiple articles
     * Optimized batch processing
     */
    fun calculateScores(articles: List<NewsArticle>): List<Float> {
        return articles.map { calculateScore(it) }
    }
    
    /**
     * Get recommended articles sorted by rule-based score
     * 
     * @param articles Pool of articles to rank
     * @param count Number of recommendations to return
     * @return Top N articles with highest scores
     */
    fun getRecommendations(
        articles: List<NewsArticle>,
        count: Int = 10
    ): List<Pair<NewsArticle, Float>> {
        return articles
            .map { article -> article to calculateScore(article) }
            .sortedByDescending { it.second }
            .take(count)
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
        
        // Normalize to 0-1 range
        return min(categoryScore / (totalScore * 0.3f), 1.0f)
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

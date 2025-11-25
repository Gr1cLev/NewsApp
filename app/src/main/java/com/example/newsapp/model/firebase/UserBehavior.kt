package com.example.newsapp.model.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Aggregated user behavior analytics
 * Collection: user_behavior/{userId}
 * 
 * This is computed/aggregated data for quick ML inference
 */
data class UserBehavior(
    @DocumentId
    val userId: String = "",
    
    // Category preferences (sorted by frequency)
    val topCategories: List<CategoryScore> = emptyList(),
    
    // Reading patterns
    val totalArticlesRead: Int = 0,
    val totalTimeSpent: Long = 0, // in seconds
    val avgReadingTime: Long = 0, // in seconds
    val totalBookmarks: Int = 0,
    val totalShares: Int = 0,
    
    // Time-based patterns
    val preferredReadingTime: String = "morning", // morning/afternoon/evening/night
    val readingFrequency: String = "daily", // daily/weekly/occasional
    val mostActiveDay: String = "Monday",
    
    // Recent activity
    val recentCategories: List<String> = emptyList(), // Last 10 categories clicked
    val recentArticleIds: List<String> = emptyList(), // Last 20 articles clicked
    
    @ServerTimestamp
    val lastActiveAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    
    // ML feature scores
    val categoryAffinityScores: Map<String, Double> = emptyMap(), // ML-computed scores
    val diversityScore: Double = 0.0, // How diverse are reading habits (0-1)
    val engagementScore: Double = 0.0 // Overall engagement (0-1)
)

/**
 * Category score for ranking
 */
data class CategoryScore(
    val category: String = "",
    val clickCount: Int = 0,
    val totalTimeSpent: Long = 0,
    val bookmarkCount: Int = 0,
    val score: Double = 0.0 // Weighted score
)

/**
 * Reading time preference
 */
enum class ReadingTimePreference {
    MORNING,    // 06:00 - 12:00
    AFTERNOON,  // 12:00 - 18:00
    EVENING,    // 18:00 - 22:00
    NIGHT       // 22:00 - 06:00
}

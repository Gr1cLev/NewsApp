package com.example.newsapp.model.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * User interaction with articles
 * Collection: user_interactions/{userId}/articles/{articleId}
 * 
 * This tracks detailed user behavior for ML model training
 */
data class UserInteraction(
    @DocumentId
    val articleId: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "",
    val source: String = "",
    
    // Interaction metrics
    val clickCount: Int = 0,
    val timeSpentReading: Long = 0, // in seconds
    val isBookmarked: Boolean = false,
    val isShared: Boolean = false,
    
    // Timestamps
    val clickedAt: List<Timestamp> = emptyList(), // All click timestamps
    @ServerTimestamp
    val lastClickedAt: Timestamp? = null,
    val bookmarkedAt: Timestamp? = null,
    val sharedAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

/**
 * Simplified version for tracking
 */
data class ArticleClick(
    val articleId: String,
    val title: String,
    val category: String,
    val source: String,
    val timestamp: Timestamp = Timestamp.now()
)

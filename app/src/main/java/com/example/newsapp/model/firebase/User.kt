package com.example.newsapp.model.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * User model for Firestore
 * Collection: users/{userId}
 */
data class User(
    @DocumentId
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val isAnonymous: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val preferences: UserPreferences = UserPreferences()
)

/**
 * User preferences (nested in User document)
 */
data class UserPreferences(
    val favoriteCategories: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val readingReminderTime: String = "", // e.g., "09:00"
    val language: String = "en"
)

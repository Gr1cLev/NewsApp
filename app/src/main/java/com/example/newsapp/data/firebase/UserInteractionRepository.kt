package com.example.newsapp.data.firebase

import com.example.newsapp.model.firebase.ArticleClick
import com.example.newsapp.model.firebase.UserInteraction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for tracking user interactions with articles
 * Collection: user_interactions/{userId}/articles/{articleId}
 */
@Singleton
class UserInteractionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) {
    
    /**
     * Track article click
     */
    suspend fun trackArticleClick(
        articleId: String,
        title: String,
        category: String,
        source: String
    ): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val articleRef = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .document(articleId)
            
            val snapshot = articleRef.get().await()
            
            if (snapshot.exists()) {
                // Update existing interaction
                articleRef.update(
                    mapOf(
                        "clickCount" to FieldValue.increment(1),
                        "clickedAt" to FieldValue.arrayUnion(Timestamp.now()),
                        "lastClickedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                // Create new interaction
                val interaction = UserInteraction(
                    articleId = articleId,
                    userId = userId,
                    title = title,
                    category = category,
                    source = source,
                    clickCount = 1,
                    clickedAt = listOf(Timestamp.now())
                )
                articleRef.set(interaction).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track reading time
     */
    suspend fun trackReadingTime(articleId: String, durationSeconds: Long): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .document(articleId)
                .update(
                    mapOf(
                        "timeSpentReading" to FieldValue.increment(durationSeconds),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track bookmark action
     */
    suspend fun trackBookmark(articleId: String, isBookmarked: Boolean): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val updates = mutableMapOf<String, Any>(
                "isBookmarked" to isBookmarked,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            if (isBookmarked) {
                updates["bookmarkedAt"] = FieldValue.serverTimestamp()
            }
            
            firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .document(articleId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track share action
     */
    suspend fun trackShare(articleId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .document(articleId)
                .update(
                    mapOf(
                        "isShared" to true,
                        "sharedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user interactions for a specific article
     */
    suspend fun getArticleInteraction(articleId: String): Result<UserInteraction?> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val document = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .document(articleId)
                .get()
                .await()
            
            Result.success(document.toObject(UserInteraction::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all user interactions
     */
    suspend fun getAllInteractions(): Result<List<UserInteraction>> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .get()
                .await()
            
            val interactions = snapshot.documents.mapNotNull { 
                it.toObject(UserInteraction::class.java) 
            }
            
            Result.success(interactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get interactions by category
     */
    suspend fun getInteractionsByCategory(category: String): Result<List<UserInteraction>> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .whereEqualTo("category", category)
                .get()
                .await()
            
            val interactions = snapshot.documents.mapNotNull { 
                it.toObject(UserInteraction::class.java) 
            }
            
            Result.success(interactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recent interactions (last N days)
     */
    suspend fun getRecentInteractions(days: Int = 7): Result<List<UserInteraction>> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val cutoffDate = Timestamp(
                System.currentTimeMillis() / 1000 - (days * 24 * 60 * 60),
                0
            )
            
            val snapshot = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .whereGreaterThan("lastClickedAt", cutoffDate)
                .orderBy("lastClickedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            val interactions = snapshot.documents.mapNotNull { 
                it.toObject(UserInteraction::class.java) 
            }
            
            Result.success(interactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get most clicked articles
     */
    suspend fun getMostClickedArticles(limit: Int = 20): Result<List<UserInteraction>> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore
                .collection("user_interactions")
                .document(userId)
                .collection("articles")
                .orderBy("clickCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val interactions = snapshot.documents.mapNotNull { 
                it.toObject(UserInteraction::class.java) 
            }
            
            Result.success(interactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

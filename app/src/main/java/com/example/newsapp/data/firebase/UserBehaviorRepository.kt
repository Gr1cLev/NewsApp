package com.example.newsapp.data.firebase

import com.example.newsapp.model.firebase.CategoryScore
import com.example.newsapp.model.firebase.UserBehavior
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for aggregated user behavior analytics
 * Collection: user_behavior/{userId}
 */
@Singleton
class UserBehaviorRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository,
    private val interactionRepository: UserInteractionRepository
) {
    
    /**
     * Get user behavior document
     */
    suspend fun getUserBehavior(): Result<UserBehavior?> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val document = firestore
                .collection("user_behavior")
                .document(userId)
                .get()
                .await()
            
            Result.success(document.toObject(UserBehavior::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize user behavior document
     */
    suspend fun initializeUserBehavior(): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val behavior = UserBehavior(
                userId = userId,
                preferredReadingTime = "morning",
                readingFrequency = "occasional"
            )
            
            firestore
                .collection("user_behavior")
                .document(userId)
                .set(behavior)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user behavior based on interactions
     * This should be called periodically or after significant events
     */
    suspend fun computeAndUpdateBehavior(): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get all interactions
            val interactionsResult = interactionRepository.getAllInteractions()
            if (interactionsResult.isFailure) {
                return Result.failure(interactionsResult.exceptionOrNull() 
                    ?: Exception("Failed to get interactions"))
            }
            
            val interactions = interactionsResult.getOrNull() ?: emptyList()
            
            if (interactions.isEmpty()) {
                return Result.success(Unit)
            }
            
            // Compute category scores
            val categoryMap = mutableMapOf<String, MutableList<Triple<Int, Long, Boolean>>>()
            interactions.forEach { interaction ->
                val data = categoryMap.getOrPut(interaction.category) { mutableListOf() }
                data.add(Triple(
                    interaction.clickCount,
                    interaction.timeSpentReading,
                    interaction.isBookmarked
                ))
            }
            
            val topCategories = categoryMap.map { (category, dataList) ->
                val totalClicks = dataList.sumOf { it.first }
                val totalTime = dataList.sumOf { it.second }
                val bookmarks = dataList.count { it.third }
                
                // Weighted score: clicks (50%) + time (30%) + bookmarks (20%)
                val score = (totalClicks * 0.5) + (totalTime / 60.0 * 0.3) + (bookmarks * 2 * 0.2)
                
                CategoryScore(
                    category = category,
                    clickCount = totalClicks,
                    totalTimeSpent = totalTime,
                    bookmarkCount = bookmarks,
                    score = score
                )
            }.sortedByDescending { it.score }.take(10)
            
            // Compute reading time preference
            val readingTimes = interactions.mapNotNull { it.lastClickedAt }
            val preferredTime = determinePreferredReadingTime(readingTimes)
            
            // Recent categories and articles
            val sortedInteractions = interactions
                .sortedByDescending { it.lastClickedAt?.seconds ?: 0 }
            val recentCategories = sortedInteractions
                .take(10)
                .map { it.category }
                .distinct()
            val recentArticleIds = sortedInteractions
                .take(20)
                .map { it.articleId }
            
            // Aggregate stats
            val totalArticlesRead = interactions.size
            val totalTimeSpent = interactions.sumOf { it.timeSpentReading }
            val avgReadingTime = if (totalArticlesRead > 0) totalTimeSpent / totalArticlesRead else 0
            val totalBookmarks = interactions.count { it.isBookmarked }
            val totalShares = interactions.count { it.isShared }
            
            // Compute diversity score (0-1)
            val uniqueCategories = interactions.map { it.category }.distinct().size
            val totalCategories = 10.0 // Assume we have ~10 categories
            val diversityScore = (uniqueCategories / totalCategories).coerceIn(0.0, 1.0)
            
            // Compute engagement score (0-1)
            val avgClicksPerArticle = interactions.map { it.clickCount }.average()
            val bookmarkRate = totalBookmarks.toDouble() / totalArticlesRead
            val engagementScore = ((avgClicksPerArticle / 5.0 * 0.5) + (bookmarkRate * 0.5)).coerceIn(0.0, 1.0)
            
            // Update user behavior document
            val behavior = UserBehavior(
                userId = userId,
                topCategories = topCategories,
                totalArticlesRead = totalArticlesRead,
                totalTimeSpent = totalTimeSpent,
                avgReadingTime = avgReadingTime,
                totalBookmarks = totalBookmarks,
                totalShares = totalShares,
                preferredReadingTime = preferredTime,
                recentCategories = recentCategories,
                recentArticleIds = recentArticleIds,
                diversityScore = diversityScore,
                engagementScore = engagementScore
            )
            
            firestore
                .collection("user_behavior")
                .document(userId)
                .set(behavior)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Determine preferred reading time based on click timestamps
     */
    private fun determinePreferredReadingTime(timestamps: List<Timestamp>): String {
        if (timestamps.isEmpty()) return "morning"
        
        val hourCounts = mutableMapOf(
            "morning" to 0,    // 06:00 - 12:00
            "afternoon" to 0,  // 12:00 - 18:00
            "evening" to 0,    // 18:00 - 22:00
            "night" to 0       // 22:00 - 06:00
        )
        
        timestamps.forEach { timestamp ->
            val calendar = Calendar.getInstance()
            calendar.time = timestamp.toDate()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            when (hour) {
                in 6..11 -> hourCounts["morning"] = hourCounts["morning"]!! + 1
                in 12..17 -> hourCounts["afternoon"] = hourCounts["afternoon"]!! + 1
                in 18..21 -> hourCounts["evening"] = hourCounts["evening"]!! + 1
                else -> hourCounts["night"] = hourCounts["night"]!! + 1
            }
        }
        
        return hourCounts.maxByOrNull { it.value }?.key ?: "morning"
    }
    
    /**
     * Get top categories for user
     */
    suspend fun getTopCategories(limit: Int = 5): Result<List<CategoryScore>> {
        return try {
            val behavior = getUserBehavior().getOrNull()
            Result.success(behavior?.topCategories?.take(limit) ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update last active timestamp
     */
    suspend fun updateLastActive(): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
            
            firestore
                .collection("user_behavior")
                .document(userId)
                .update("lastActiveAt", FieldValue.serverTimestamp())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

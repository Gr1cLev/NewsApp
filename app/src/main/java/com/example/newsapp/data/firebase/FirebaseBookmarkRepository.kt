package com.example.newsapp.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user bookmarks in Firestore
 * Replaces local JSON file storage with cloud-based Firestore
 * 
 * Firestore structure:
 * user_bookmarks/{userId}/bookmarks/{articleId}
 *   - articleId: String (ID of bookmarked article)
 *   - bookmarkedAt: Long (timestamp when bookmarked)
 *   - title: String (article title for display)
 *   - category: String (article category)
 */
@Singleton
class FirebaseBookmarkRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "FirebaseBookmarkRepo"
        private const val COLLECTION_BOOKMARKS = "user_bookmarks"
        private const val SUBCOLLECTION_BOOKMARKS = "bookmarks"
    }
    
    /**
     * Get current user ID (supports anonymous, email, and Google auth)
     */
    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
    
    /**
     * Load all bookmark IDs for current user from Firestore
     * Returns empty set if user not authenticated or no bookmarks
     */
    suspend fun loadBookmarks(): Set<Int> {
        val userId = getCurrentUserId() ?: return emptySet()
        
        return try {
            val snapshot = firestore
                .collection(COLLECTION_BOOKMARKS)
                .document(userId)
                .collection(SUBCOLLECTION_BOOKMARKS)
                .get()
                .await()
            
            val bookmarkIds = snapshot.documents.mapNotNull { doc ->
                doc.id.toIntOrNull()
            }.toSet()
            
            Log.d(TAG, "✅ Loaded ${bookmarkIds.size} bookmarks for user $userId from Firestore")
            bookmarkIds
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load bookmarks from Firestore: ${e.message}")
            emptySet()
        }
    }
    
    /**
     * Add bookmark to Firestore
     * Stores article metadata for analytics and display
     */
    suspend fun addBookmark(
        articleId: Int,
        title: String = "",
        category: String = ""
    ): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val bookmarkData = mapOf(
                "articleId" to articleId,
                "bookmarkedAt" to System.currentTimeMillis(),
                "title" to title,
                "category" to category
            )
            
            firestore
                .collection(COLLECTION_BOOKMARKS)
                .document(userId)
                .collection(SUBCOLLECTION_BOOKMARKS)
                .document(articleId.toString())
                .set(bookmarkData)
                .await()
            
            Log.d(TAG, "✅ Bookmark added to Firestore: articleId=$articleId, userId=$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add bookmark to Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Remove bookmark from Firestore
     */
    suspend fun removeBookmark(articleId: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            firestore
                .collection(COLLECTION_BOOKMARKS)
                .document(userId)
                .collection(SUBCOLLECTION_BOOKMARKS)
                .document(articleId.toString())
                .delete()
                .await()
            
            Log.d(TAG, "✅ Bookmark removed from Firestore: articleId=$articleId, userId=$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove bookmark from Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if article is bookmarked
     */
    suspend fun isBookmarked(articleId: Int): Boolean {
        val userId = getCurrentUserId() ?: return false
        
        return try {
            val doc = firestore
                .collection(COLLECTION_BOOKMARKS)
                .document(userId)
                .collection(SUBCOLLECTION_BOOKMARKS)
                .document(articleId.toString())
                .get()
                .await()
            
            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check bookmark status: ${e.message}")
            false
        }
    }
    
    /**
     * Sync local bookmarks to Firestore (one-time migration)
     * Called when user first authenticates to migrate local JSON data
     */
    suspend fun syncLocalToFirestore(localBookmarkIds: Set<Int>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        
        if (localBookmarkIds.isEmpty()) {
            Log.d(TAG, "No local bookmarks to sync")
            return Result.success(Unit)
        }
        
        return try {
            val batch = firestore.batch()
            val userBookmarksRef = firestore
                .collection(COLLECTION_BOOKMARKS)
                .document(userId)
                .collection(SUBCOLLECTION_BOOKMARKS)
            
            localBookmarkIds.forEach { articleId ->
                val docRef = userBookmarksRef.document(articleId.toString())
                val bookmarkData = mapOf(
                    "articleId" to articleId,
                    "bookmarkedAt" to System.currentTimeMillis(),
                    "title" to "",
                    "category" to "",
                    "migratedFromLocal" to true
                )
                batch.set(docRef, bookmarkData)
            }
            
            batch.commit().await()
            Log.d(TAG, "✅ Synced ${localBookmarkIds.size} local bookmarks to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync local bookmarks to Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clear all bookmarks for current user (used on logout)
     * Note: This only clears local cache, Firestore data remains
     */
    fun clearLocalCache() {
        Log.d(TAG, "Local bookmark cache cleared")
    }
}

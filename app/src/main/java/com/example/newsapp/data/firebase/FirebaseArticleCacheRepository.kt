package com.example.newsapp.data.firebase

import android.util.Log
import com.example.newsapp.model.NewsArticle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for caching articles in Firestore
 * 
 * Strategy:
 * 1. Fetch dari API → Cache semua artikel ke Firestore
 * 2. API rate limit (429) → Load dari cache
 * 3. Search → Cari di cache dulu, fallback ke API
 * 4. Bookmark → Data artikel sudah ada di cache
 * 
 * Firestore structure:
 * articles_cache/{articleId}
 *   - id: Int
 *   - category: String
 *   - title: String
 *   - summary: String
 *   - source: String
 *   - publishedAt: String
 *   - accentColor: Int
 *   - heroImageUrl: String?
 *   - tag: String
 *   - isFeatured: Boolean
 *   - cachedAt: Long (timestamp)
 *   - fetchedFrom: String (API, search, etc.)
 */
@Singleton
class FirebaseArticleCacheRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "ArticleCacheRepo"
        private const val COLLECTION_CACHE = "articles_cache"
        private const val CACHE_EXPIRY_DAYS = 7 // Cache expires after 7 days
    }
    
    /**
     * Cache articles to Firestore (batch write for performance)
     * Called after successful API fetch
     */
    suspend fun cacheArticles(
        articles: List<NewsArticle>,
        source: String = "API"
    ): Result<Unit> {
        if (articles.isEmpty()) return Result.success(Unit)
        
        return try {
            val batch = firestore.batch()
            val timestamp = System.currentTimeMillis()
            
            articles.forEach { article ->
                val docRef = firestore
                    .collection(COLLECTION_CACHE)
                    .document(article.id.toString())
                
                val articleData = mapOf(
                    "id" to article.id,
                    "category" to article.category,
                    "title" to article.title,
                    "summary" to article.summary,
                    "source" to article.source,
                    "publishedAt" to article.publishedAt,
                    "accentColor" to article.accentColor,
                    "heroImageUrl" to article.heroImageUrl,
                    "tag" to article.tag,
                    "isFeatured" to article.isFeatured,
                    "content" to article.content,
                    "cachedAt" to timestamp,
                    "fetchedFrom" to source
                )
                
                batch.set(docRef, articleData)
            }
            
            batch.commit().await()
            Log.d(TAG, "✅ Cached ${articles.size} articles to Firestore from $source")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cache articles: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get single article from cache by ID
     * Used for bookmarks when API unavailable
     */
    suspend fun getArticleById(articleId: Int): NewsArticle? {
        return try {
            val doc = firestore
                .collection(COLLECTION_CACHE)
                .document(articleId.toString())
                .get()
                .await()
            
            if (doc.exists()) {
                documentToArticle(doc.data)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get article $articleId from cache: ${e.message}")
            null
        }
    }
    
    /**
     * Get multiple articles by IDs
     * Used for bookmark list when API unavailable
     */
    suspend fun getArticlesByIds(articleIds: Set<Int>): List<NewsArticle> {
        if (articleIds.isEmpty()) return emptyList()
        
        return try {
            // Firestore 'in' query limit is 10, so batch requests
            val articles = mutableListOf<NewsArticle>()
            articleIds.chunked(10).forEach { chunk ->
                val snapshot = firestore
                    .collection(COLLECTION_CACHE)
                    .whereIn("id", chunk)
                    .get()
                    .await()
                
                snapshot.documents.mapNotNull { doc ->
                    documentToArticle(doc.data)
                }.let { articles.addAll(it) }
            }
            
            Log.d(TAG, "✅ Retrieved ${articles.size}/${articleIds.size} articles from cache")
            articles
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get articles from cache: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get recent cached articles (fallback when API rate limited)
     * Returns up to 100 most recent articles
     */
    suspend fun getRecentArticles(limit: Int = 100): List<NewsArticle> {
        return try {
            val snapshot = firestore
                .collection(COLLECTION_CACHE)
                .orderBy("cachedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val articles = snapshot.documents.mapNotNull { doc ->
                documentToArticle(doc.data)
            }
            
            Log.d(TAG, "✅ Retrieved ${articles.size} recent articles from cache")
            articles
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get recent articles: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search articles in cache by title/description
     * Used when API rate limited or as first search attempt
     */
    suspend fun searchInCache(query: String, limit: Int = 50): List<NewsArticle> {
        if (query.isBlank()) return emptyList()
        
        return try {
            // Firestore doesn't support full-text search, so we get all recent and filter locally
            val snapshot = firestore
                .collection(COLLECTION_CACHE)
                .orderBy("cachedAt", Query.Direction.DESCENDING)
                .limit(500) // Get more to search within
                .get()
                .await()
            
            val searchTerms = query.lowercase().split(" ").filter { it.length > 2 }
            
            val articles = snapshot.documents.mapNotNull { doc ->
                documentToArticle(doc.data)
            }.filter { article ->
                // Match if any search term found in title or summary
                searchTerms.any { term ->
                    article.title.lowercase().contains(term) ||
                    article.summary.lowercase().contains(term)
                }
            }.take(limit)
            
            Log.d(TAG, "✅ Found ${articles.size} articles in cache for query: $query")
            articles
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to search in cache: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get articles by category from cache
     */
    suspend fun getArticlesByCategory(category: String, limit: Int = 50): List<NewsArticle> {
        return try {
            val snapshot = firestore
                .collection(COLLECTION_CACHE)
                .whereEqualTo("category", category)
                .orderBy("cachedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val articles = snapshot.documents.mapNotNull { doc ->
                documentToArticle(doc.data)
            }
            
            Log.d(TAG, "✅ Retrieved ${articles.size} articles for category $category from cache")
            articles
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get articles by category: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Delete old cached articles (cleanup)
     * Called periodically to prevent cache bloat
     */
    suspend fun cleanupOldCache(): Result<Int> {
        return try {
            val expiryTime = System.currentTimeMillis() - (CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000)
            
            val snapshot = firestore
                .collection(COLLECTION_CACHE)
                .whereLessThan("cachedAt", expiryTime)
                .get()
                .await()
            
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            
            val deletedCount = snapshot.size()
            Log.d(TAG, "✅ Cleaned up $deletedCount old cached articles")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cleanup cache: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Convert Firestore document to NewsArticle
     */
    private fun documentToArticle(data: Map<String, Any?>?): NewsArticle? {
        if (data == null) return null
        
        return try {
            @Suppress("UNCHECKED_CAST")
            val contentList = (data["content"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            NewsArticle(
                id = (data["id"] as? Long)?.toInt() ?: 0,
                category = data["category"] as? String ?: "",
                title = data["title"] as? String ?: "",
                summary = data["summary"] as? String ?: "",
                source = data["source"] as? String ?: "",
                publishedAt = data["publishedAt"] as? String ?: "",
                accentColor = (data["accentColor"] as? Long)?.toInt() ?: 0,
                heroImageUrl = data["heroImageUrl"] as? String,
                tag = data["tag"] as? String ?: data["category"] as? String ?: "",
                isFeatured = data["isFeatured"] as? Boolean ?: false,
                content = contentList
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to convert document to article: ${e.message}")
            null
        }
    }
}

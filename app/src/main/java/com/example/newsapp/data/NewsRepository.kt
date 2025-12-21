package com.example.newsapp.data

import android.content.Context
import android.util.Log
import com.example.newsapp.data.firebase.FirebaseArticleCacheRepository
import com.example.newsapp.data.firebase.FirebaseBookmarkRepository
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.model.NewsData
import com.example.newsapp.network.NewsApiService
import com.example.newsapp.network.mapper.NewsMapper
import com.example.newsapp.network.mapper.NewsMapper.toNewsArticle
import com.example.newsapp.util.Resource
import com.example.newsapp.util.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository - API + Firestore Cache + Bookmarks
 * Strategy:
 * 1. Fetch API → Cache to Firestore
 * 2. API 429 rate limit → Load from Firestore cache
 * 3. Search → Cache first, then API
 * 4. Bookmarks → Load full articles from cache
 */
@Singleton
class NewsRepository @Inject constructor(
    private val context: Context,
    private val newsApiService: NewsApiService,
    private val firebaseBookmarkRepository: FirebaseBookmarkRepository,
    private val firebaseArticleCacheRepository: FirebaseArticleCacheRepository
) {
    
    companion object {
        private const val TAG = "NewsRepository"
        private const val PREFS_NAME = "news_cache"
        private const val KEY_MULTI_CATEGORY_TIMESTAMP = "multi_category_timestamp"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun invalidateCache() {
        cachedData = null
        articleIndex = emptyMap()
        bookmarkIds.clear()
        cachedBookmarkProfileId = null
        cachedMultiCategoryArticles = null
        prefs.edit().remove(KEY_MULTI_CATEGORY_TIMESTAMP).apply()
    }

    
    @Volatile
    private var cachedData: NewsData? = null
    private val bookmarkIds = mutableSetOf<Int>()
    private var articleIndex: Map<Int, NewsArticle> = emptyMap()
    @Volatile
    private var cachedBookmarkProfileId: String? = null
    @Volatile
    private var cachedMultiCategoryArticles: List<NewsArticle>? = null
    
    /**
     * Fetch articles dari multiple categories untuk ML recommendations
     * Returns diverse articles across Sports, Business, Technology, etc.
     * Implements 30-minute cache TTL to reduce API calls.
     */
    suspend fun fetchMultiCategoryArticles(country: String = "us", forceRefresh: Boolean = false): Resource<List<NewsArticle>> = withContext(Dispatchers.IO) {
        // Check cache first (unless force refresh)
        if (!forceRefresh) {
            val lastFetchTime = prefs.getLong(KEY_MULTI_CATEGORY_TIMESTAMP, 0L)
            val cacheAge = System.currentTimeMillis() - lastFetchTime
            
            if (cacheAge < CACHE_TTL_MS && cachedMultiCategoryArticles != null) {
                Log.d(TAG, "✅ Using cached multi-category articles (age: ${cacheAge / 1000}s)")
                return@withContext Resource.Success(cachedMultiCategoryArticles!!)
            }
        }
        
        // Fetch fresh data
        val categories = listOf("sports", "business", "technology", "health", "entertainment")
        val allArticles = mutableListOf<NewsArticle>()
        var baseTimestamp = System.currentTimeMillis()
        
        categories.forEach { category ->
            try {
                val result = fetchArticlesFromNetwork(category = category, country = country)
                if (result is Resource.Success) {
                    // Adjust IDs to ensure uniqueness across categories
                    val adjustedArticles = result.data.map { article ->
                        article.copy(id = kotlin.math.abs(article.title.hashCode()) + baseTimestamp.toInt())
                            .also { baseTimestamp++ }
                    }
                    allArticles.addAll(adjustedArticles)
                    Log.d(TAG, "📦 Fetched ${adjustedArticles.size} articles for category: $category")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to fetch $category: ${e.message}")
            }
        }
        
        // Update cache
        cachedMultiCategoryArticles = allArticles
        prefs.edit().putLong(KEY_MULTI_CATEGORY_TIMESTAMP, System.currentTimeMillis()).apply()
        
        Log.d(TAG, "🎯 Multi-category fetch complete: ${allArticles.size} total articles (cache updated)")
        Resource.Success(allArticles)
    }
    
    /**
     * Fetch articles dari network (NewsAPI)
     * 5 artikel pertama akan di-set sebagai featured
     */
    suspend fun fetchArticlesFromNetwork(
        category: String? = null,
        country: String = "id"
    ): Resource<List<NewsArticle>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val apiCategory = NewsMapper.mapUiCategoryToApiCategory(category ?: "")
            val response = newsApiService.getTopHeadlines(
                country = country,
                category = apiCategory,
                pageSize = 50
            )
            
            if (response.isSuccessful && response.body() != null) {
                // Determine display category: use requested category or "General" for "Top"
                val displayCategory = when {
                    category.isNullOrEmpty() -> "General"
                    category.equals("Top", ignoreCase = true) -> "General"
                    else -> category.replaceFirstChar { it.uppercaseChar() } // Capitalize: sports -> Sports
                }
                
                val articles = response.body()!!.articles.mapIndexed { index, dto ->
                    dto.toNewsArticle(
                        id = kotlin.math.abs((dto.url ?: dto.title).hashCode()) + index,
                        category = displayCategory,
                        isFeatured = index < 5 // 5 artikel pertama = featured
                    )
                }
                
                // Update articleIndex with fetched articles
                val currentIndex = articleIndex.toMutableMap()
                articles.forEach { article ->
                    currentIndex[article.id] = article
                }
                articleIndex = currentIndex
                
                Log.d(TAG, "✅ Fetched ${articles.size} articles (category: $displayCategory, featured: ${articles.count { it.isFeatured }})")
                articles
            } else {
                throw Exception("API Error: ${response.code()} - ${response.message()}")
            }
        }
    }
    
    /**
     * Search articles dari network
     */
    suspend fun searchArticles(query: String, category: String = "General"): Resource<List<NewsArticle>> = withContext(Dispatchers.IO) {
        val result = safeApiCall {
            val response = newsApiService.searchEverything(
                query = query,
                language = "en", // Change to English for more results
                pageSize = 50
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.mapIndexed { index, dto ->
                    dto.toNewsArticle(
                        id = kotlin.math.abs((dto.url ?: dto.title).hashCode()) + index,
                        category = category,
                        isFeatured = index < 5 // 5 pertama = featured
                    )
                }
                Log.d(TAG, "Search returned ${articles.size} articles")
                
                // Cache search results to Firestore for fallback
                if (articles.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        firebaseArticleCacheRepository.cacheArticles(articles, "Search-$query")
                    }
                }
                
                articles
            } else {
                // Check if it's rate limit error (429)
                if (response.code() == 429) {
                    Log.w(TAG, "⚠️ Search rate limited, loading from cache...")
                    val cachedArticles = firebaseArticleCacheRepository.getRecentArticles(limit = 50)
                    return@safeApiCall cachedArticles.filter { 
                        it.title.contains(query, ignoreCase = true) || 
                        it.summary.contains(query, ignoreCase = true)
                    }
                }
                throw Exception("Search Error: ${response.code()}")
            }
        }

        // Update article index so detail screens can resolve by ID
        if (result is Resource.Success) {
            val updatedIndex = articleIndex.toMutableMap()
            result.data.forEach { article ->
                updatedIndex[article.id] = article
            }
            articleIndex = updatedIndex
        }

        result
    }

    /**
     * Get news data - WAJIB dari API (no fallback)
     * Cache hasil untuk performa
     */
    suspend fun getNewsData(): NewsData {
        // Check cache first
        val existing = cachedData
        if (existing != null) {
            Log.d(TAG, "Using cached data")
            // Bookmarks loaded on-demand via getBookmarks()
            return existing
        }
        
        // Fetch dari API - MULTI CATEGORY untuk ML recommendations
        Log.d(TAG, "Fetching fresh data from API (multi-category)...")
        
        // Try multi-category fetch first for diverse data
        var networkResult = fetchMultiCategoryArticles(country = "us")
        
        // If multi-category returns empty, fallback to Indonesia General
        if (networkResult is Resource.Success && networkResult.data.isEmpty()) {
            Log.w(TAG, "⚠️ Multi-category returned 0 articles, trying Indonesia General...")
            networkResult = fetchArticlesFromNetwork(category = "Top", country = "id")
        }
        
        // If still 0, try general search
        if (networkResult is Resource.Success && networkResult.data.isEmpty()) {
            Log.w(TAG, "⚠️ Still 0 articles, trying general search...")
            networkResult = searchArticles("breaking news OR trending", category = "General")
        }
        
        return when (networkResult) {
            is Resource.Success -> {
                val allArticles = networkResult.data
                val featured = allArticles.filter { it.isFeatured }
                
                // Log category distribution
                val categoryCount = allArticles.groupingBy { it.category }.eachCount()
                Log.d(TAG, "✅ API Success: ${allArticles.size} articles, ${featured.size} featured")
                Log.d(TAG, "📊 Category distribution: $categoryCount")
                
                // Cache articles to Firestore for fallback
                withContext(Dispatchers.IO) {
                    firebaseArticleCacheRepository.cacheArticles(allArticles, "API-MultiCategory")
                }
                
                val newsData = NewsData(
                    categories = defaultCategories(),
                    featuredArticles = featured,
                    articles = allArticles,
                    bookmarkedArticles = emptyList(),
                    searchSuggestions = allArticles.take(10).map { it.title }
                )
                
                // Cache
                cachedData = newsData
                articleIndex = allArticles.associateBy { it.id }
                // Bookmarks loaded on-demand via getBookmarks()
                
                cachedData ?: newsData
            }
            is Resource.Error -> {
                Log.e(TAG, "❌ API Failed: ${networkResult.message}")
                
                // Check if it's rate limit error (429)
                if (networkResult.message?.contains("429") == true || 
                    networkResult.message?.contains("rate limit", ignoreCase = true) == true) {
                    Log.w(TAG, "⚠️ Rate limit detected, loading from Firestore cache...")
                    return loadFromCache()
                }
                
                throw Exception("Failed to fetch news: ${networkResult.message}")
            }
            is Resource.Loading -> {
                throw Exception("Loading state tidak valid di sini")
            }
        }
    }

    fun getCategories(): List<NewsCategory> =
        cachedData?.categories ?: defaultCategories()

    fun getFeaturedArticles(): List<NewsArticle> =
        cachedData?.featuredArticles ?: emptyList()

    fun getArticles(): List<NewsArticle> =
        cachedData?.articles ?: emptyList()

    suspend fun getCachedArticlesByCategory(category: String): List<NewsArticle> {
        // Try memory first
        val fromCache = cachedData?.articles?.filter { it.category.equals(category, ignoreCase = true) }
        if (!fromCache.isNullOrEmpty()) return fromCache

        // Firestore fallback
        val cached = firebaseArticleCacheRepository.getArticlesByCategory(category)
        if (cached.isNotEmpty()) {
            val updatedIndex = articleIndex.toMutableMap()
            cached.forEach { updatedIndex[it.id] = it }
            articleIndex = updatedIndex
        }
        return cached
    }

    suspend fun getBookmarks(): List<NewsArticle> {
        // Load bookmark IDs from Firestore
        val firestoreBookmarks = firebaseBookmarkRepository.loadBookmarks()
        bookmarkIds.clear()
        bookmarkIds.addAll(firestoreBookmarks)
        
        if (firestoreBookmarks.isEmpty()) return emptyList()
        
        // Try to get articles from current cache (articleIndex)
        val articlesFromIndex = firestoreBookmarks.mapNotNull { articleIndex[it] }
        
        // If we got all articles from index, return them
        if (articlesFromIndex.size == firestoreBookmarks.size) {
            Log.d(TAG, "✅ Loaded ${articlesFromIndex.size} bookmarks from memory cache")
            refreshCachedBookmarks()
            return articlesFromIndex
        }
        
        // Otherwise, fetch missing articles from Firestore cache
        Log.d(TAG, "⚠️ Some bookmarks not in memory, fetching from Firestore cache...")
        val articlesFromCache = firebaseArticleCacheRepository.getArticlesByIds(firestoreBookmarks)
        
        Log.d(TAG, "✅ Loaded ${articlesFromCache.size}/${firestoreBookmarks.size} bookmarks from Firestore cache")
        return articlesFromCache
    }

    suspend fun getArticleById(articleId: Int): NewsArticle? {
        // If articleIndex is empty, try to load data first
        if (articleIndex.isEmpty() && cachedData == null) {
            try {
                getNewsData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load data for getArticleById: ${e.message}")
                return null
            }
        }

        // Return from in-memory index if present
        articleIndex[articleId]?.let { return it }

        // Fallback to Firestore cache so deep links/search/bookmarks still resolve
        val cachedArticle = firebaseArticleCacheRepository.getArticleById(articleId)
        if (cachedArticle != null) {
            val updatedIndex = articleIndex.toMutableMap()
            updatedIndex[articleId] = cachedArticle
            articleIndex = updatedIndex
            return cachedArticle
        }

        Log.w(TAG, "Article $articleId not found in repository or cache")
        return null
    }

    fun getSearchSuggestions(): List<String> =
        cachedData?.searchSuggestions ?: emptyList()

    fun isArticleBookmarked(articleId: Int): Boolean {
        // Use local cache (already loaded from Firestore via getBookmarks())
        return bookmarkIds.contains(articleId)
    }

    suspend fun toggleBookmark(articleId: Int): Boolean {
        val isCurrentlyBookmarked = bookmarkIds.contains(articleId)
        val isBookmarked: Boolean
        
        if (isCurrentlyBookmarked) {
            // Remove bookmark
            bookmarkIds.remove(articleId)
            firebaseBookmarkRepository.removeBookmark(articleId)
            isBookmarked = false
            Log.d(TAG, "🔖 Bookmark removed: articleId=$articleId")
        } else {
            // Add bookmark with article metadata
            val article = getArticleById(articleId)
            bookmarkIds.add(articleId)
            firebaseBookmarkRepository.addBookmark(
                articleId = articleId,
                title = article?.title ?: "",
                category = article?.category ?: ""
            )
            isBookmarked = true
            Log.d(TAG, "🔖 Bookmark added: articleId=$articleId")
        }
        
        refreshCachedBookmarks()
        return isBookmarked
    }

    // ===== Private Helper Methods =====

    private fun refreshCachedBookmarks() {
        cachedData = cachedData?.copy(bookmarkedArticles = recomputeBookmarks())
    }

    private fun recomputeBookmarks(): List<NewsArticle> =
        bookmarkIds.mapNotNull { articleIndex[it] }

    private fun defaultCategories(): List<NewsCategory> = listOf(
        NewsCategory(0, "Top"),
        NewsCategory(2, "Sports"),
        NewsCategory(3, "Business"),
        NewsCategory(4, "Entertainment"),
        NewsCategory(5, "Technology"),
        NewsCategory(6, "Health"),
        NewsCategory(7, "Science")
    )

    /**
     * Load articles from Firestore cache (fallback when API rate limited)
     */
    private suspend fun loadFromCache(): NewsData {
        Log.d(TAG, "📦 Loading from Firestore cache...")
        val cachedArticles = firebaseArticleCacheRepository.getRecentArticles(limit = 100)
        
        if (cachedArticles.isEmpty()) {
            throw Exception("No cached articles available and API rate limited")
        }
        
        val featured = cachedArticles.filter { it.isFeatured }
        Log.d(TAG, "✅ Loaded ${cachedArticles.size} articles from cache, ${featured.size} featured")
        
        val newsData = NewsData(
            categories = defaultCategories(),
            featuredArticles = featured,
            articles = cachedArticles,
            bookmarkedArticles = emptyList(),
            searchSuggestions = cachedArticles.take(10).map { it.title }
        )
        
        cachedData = newsData
        articleIndex = cachedArticles.associateBy { it.id }
        
        return newsData
    }
}



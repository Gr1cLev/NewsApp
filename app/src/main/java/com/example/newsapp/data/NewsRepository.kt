package com.example.newsapp.data

import android.content.Context
import android.util.Log
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
 * Repository - 100% API NewsAPI.org
 * No local JSON fallback
 */
@Singleton
class NewsRepository @Inject constructor(
    private val context: Context,
    private val newsApiService: NewsApiService
) {
    
    companion object {
        private const val TAG = "NewsRepository"
    }

    fun invalidateCache() {
        cachedData = null
        articleIndex = emptyMap()
        bookmarkIds.clear()
        cachedBookmarkProfileId = null
    }

    
    @Volatile
    private var cachedData: NewsData? = null
    private val bookmarkIds = mutableSetOf<Int>()
    private var articleIndex: Map<Int, NewsArticle> = emptyMap()
    @Volatile
    private var cachedBookmarkProfileId: String? = null
    
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
                // Determine display category: use requested category or "General" for "All"/"Top"
                val displayCategory = when {
                    category.isNullOrEmpty() -> "General"
                    category.equals("All", ignoreCase = true) -> "General"
                    category.equals("Top", ignoreCase = true) -> "General"
                    else -> category
                }
                
                val articles = response.body()!!.articles.mapIndexed { index, dto ->
                    dto.toNewsArticle(
                        id = System.currentTimeMillis().toInt() + index,
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
        safeApiCall {
            val response = newsApiService.searchEverything(
                query = query,
                language = "en", // Change to English for more results
                pageSize = 50
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.mapIndexed { index, dto ->
                    dto.toNewsArticle(
                        id = System.currentTimeMillis().toInt() + index,
                        category = category,
                        isFeatured = index < 5 // 5 pertama = featured
                    )
                }
                Log.d(TAG, "Search returned ${articles.size} articles")
                articles
            } else {
                throw Exception("Search Error: ${response.code()}")
            }
        }
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
            ensureBookmarksLoaded()
            return existing
        }
        
        // Fetch dari API - COBA BEBERAPA COUNTRY
        Log.d(TAG, "Fetching fresh data from API...")
        
        // Try Indonesia first with All category
        var networkResult = fetchArticlesFromNetwork(category = "All", country = "id")
        
        // If Indonesia returns 0, try US
        if (networkResult is Resource.Success && networkResult.data.isEmpty()) {
            Log.w(TAG, "⚠️ Indonesia returned 0 articles, trying US...")
            networkResult = fetchArticlesFromNetwork(category = "All", country = "us")
        }
        
        // If still 0, try general search
        if (networkResult is Resource.Success && networkResult.data.isEmpty()) {
            Log.w(TAG, "⚠️ US also returned 0, trying general search...")
            networkResult = searchArticles("breaking news OR trending", category = "General")
        }
        
        return when (networkResult) {
            is Resource.Success -> {
                val allArticles = networkResult.data
                val featured = allArticles.filter { it.isFeatured }
                
                Log.d(TAG, "✅ API Success: ${allArticles.size} articles, ${featured.size} featured")
                
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
                ensureBookmarksLoaded()
                
                cachedData ?: newsData
            }
            is Resource.Error -> {
                Log.e(TAG, "❌ API Failed: ${networkResult.message}")
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

    fun getBookmarks(): List<NewsArticle> {
        if (cachedData == null) return emptyList()
        ensureBookmarksLoaded()
        return cachedData?.bookmarkedArticles ?: emptyList()
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
        return articleIndex[articleId]
    }

    fun getSearchSuggestions(): List<String> =
        cachedData?.searchSuggestions ?: emptyList()

    fun isArticleBookmarked(articleId: Int): Boolean {
        ensureBookmarksLoaded()
        return bookmarkIds.contains(articleId)
    }

    fun toggleBookmark(articleId: Int): Boolean {
        val appContext = context.applicationContext
        ensureBookmarksLoaded()
        val profileId = resolveProfileId(appContext)
        val isBookmarked = if (bookmarkIds.contains(articleId)) {
            bookmarkIds.remove(articleId)
            false
        } else {
            bookmarkIds.add(articleId)
            true
        }
        BookmarkRepository.persistBookmarks(appContext, profileId, bookmarkIds)
        refreshCachedBookmarks()
        return isBookmarked
    }

    // ===== Private Helper Methods =====

    private fun refreshCachedBookmarks() {
        cachedData = cachedData?.copy(bookmarkedArticles = recomputeBookmarks())
    }

    private fun recomputeBookmarks(): List<NewsArticle> =
        bookmarkIds.mapNotNull { articleIndex[it] }

    private fun resolveProfileId(context: Context): String {
        val profile = ProfileRepository.getActiveProfile(context)
        return profile?.id ?: BookmarkRepository.GUEST_PROFILE_ID
    }

    private fun ensureBookmarksLoaded() {
        val appContext = context.applicationContext
        val profileId = resolveProfileId(appContext)
        if (profileId == cachedBookmarkProfileId && cachedData != null) {
            return
        }
        val defaults = if (cachedBookmarkProfileId == null) bookmarkIds.toSet() else emptySet()
        val stored = BookmarkRepository.readBookmarks(appContext, profileId)
        bookmarkIds.clear()
        when {
            stored.isNotEmpty() -> bookmarkIds.addAll(stored)
            defaults.isNotEmpty() -> {
                bookmarkIds.addAll(defaults)
                BookmarkRepository.persistBookmarks(appContext, profileId, defaults)
            }
        }
        cachedBookmarkProfileId = profileId
        refreshCachedBookmarks()
    }

    private fun defaultCategories(): List<NewsCategory> = listOf(
        NewsCategory(0, "All"),
        NewsCategory(1, "Top"),
        NewsCategory(2, "Sports"),
        NewsCategory(3, "Business"),
        NewsCategory(4, "Entertainment"),
        NewsCategory(5, "Technology"),
        NewsCategory(6, "Health"),
        NewsCategory(7, "Science")
    )
}



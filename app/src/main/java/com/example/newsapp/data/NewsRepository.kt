package com.example.newsapp.data

import android.content.Context
import android.graphics.Color
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.model.NewsCategory
import com.example.newsapp.model.NewsData
import org.json.JSONArray
import org.json.JSONObject

object NewsRepository {

    private const val DATA_FILE_NAME = "news_data.json"
    @Volatile
    private var cachedData: NewsData? = null
    private val bookmarkIds = mutableSetOf<Int>()
    private var articleIndex: Map<Int, NewsArticle> = emptyMap()

    fun getNewsData(context: Context): NewsData = getData(context)

    fun getCategories(context: Context): List<NewsCategory> =
        getData(context).categories

    fun getFeaturedArticles(context: Context): List<NewsArticle> =
        getData(context).featuredArticles

    fun getArticles(context: Context): List<NewsArticle> =
        getData(context).articles

    fun getBookmarks(context: Context): List<NewsArticle> =
        ensureData(context).bookmarkedArticles

    fun getArticleById(context: Context, articleId: Int): NewsArticle? {
        ensureData(context)
        return articleIndex[articleId]
    }

    fun getSearchSuggestions(context: Context): List<String> =
        getData(context).searchSuggestions

    fun isArticleBookmarked(context: Context, articleId: Int): Boolean {
        ensureData(context)
        return bookmarkIds.contains(articleId)
    }

    fun toggleBookmark(context: Context, articleId: Int): Boolean {
        ensureData(context)
        val isBookmarked = if (bookmarkIds.contains(articleId)) {
            bookmarkIds.remove(articleId)
            false
        } else {
            bookmarkIds.add(articleId)
            true
        }
        refreshCachedBookmarks()
        return isBookmarked
    }

    private fun getData(context: Context): NewsData {
        val existing = cachedData
        if (existing != null) return existing

        return synchronized(this) {
            val doubleCheck = cachedData
            if (doubleCheck != null) {
                doubleCheck
            } else {
                val parsed = loadData(context.applicationContext)
                cachedData = parsed
                parsed
            }
        }
    }

    private fun ensureData(context: Context): NewsData = getData(context)

    private fun loadData(context: Context): NewsData {
        val jsonString = context.assets.open(DATA_FILE_NAME).bufferedReader().use { it.readText() }
        return parseJson(jsonString)
    }

    private fun parseJson(rawJson: String): NewsData {
        val root = JSONObject(rawJson)

        val articles = root.optJSONArray("articles").toArticleList()
        val categories = root.optJSONArray("categories").toCategoryList()
        val featuredArticles = root.optJSONArray("featured_articles").toArticleList()

        val bookmarkIds = root.optJSONArray("bookmarked_article_ids").toIntList()
        val articlePool = (articles + featuredArticles).associateBy { it.id }
        articleIndex = articlePool
        this.bookmarkIds.clear()
        this.bookmarkIds.addAll(bookmarkIds)
        val bookmarks = recomputeBookmarks()

        val suggestions = root.optJSONArray("search_suggestions").toStringList()

        return NewsData(
            categories = if (categories.isNotEmpty()) categories else defaultCategories(),
            featuredArticles = if (featuredArticles.isNotEmpty()) featuredArticles else articles.take(3),
            articles = articles,
            bookmarkedArticles = bookmarks,
            searchSuggestions = suggestions
        )
    }

    private fun refreshCachedBookmarks() {
        cachedData = cachedData?.copy(bookmarkedArticles = recomputeBookmarks())
    }

    private fun recomputeBookmarks(): List<NewsArticle> =
        bookmarkIds.mapNotNull { articleIndex[it] }

    private fun JSONArray?.toCategoryList(): List<NewsCategory> = this?.let { array ->
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(
                    NewsCategory(
                        id = obj.optInt("id", index),
                        name = obj.optString("name", "Category")
                    )
                )
            }
        }
    } ?: emptyList()

    private fun JSONArray?.toArticleList(): List<NewsArticle> = this?.let { array ->
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(parseArticle(obj))
            }
        }
    } ?: emptyList()

    private fun JSONArray?.toIntList(): List<Int> = this?.let { array ->
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(array.optInt(index))
            }
        }
    } ?: emptyList()

    private fun JSONArray?.toStringList(): List<String> = this?.let { array ->
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (!value.isNullOrBlank()) add(value)
            }
        }
    } ?: emptyList()

    private fun parseArticle(obj: JSONObject): NewsArticle {
        val colorString = obj.optString("accentColor", "#00AEEF")
        val accentColor = runCatching { Color.parseColor(colorString) }.getOrDefault(Color.parseColor("#00AEEF"))
        val heroImageUrl = obj.optString("heroImageUrl").takeIf { it.isNotBlank() }
        val tagValue = obj.optString("tag").takeIf { it.isNotBlank() }

        return NewsArticle(
            id = obj.optInt("id"),
            category = obj.optString("category"),
            title = obj.optString("title"),
            summary = obj.optString("summary"),
            source = obj.optString("source"),
            publishedAt = obj.optString("publishedAt"),
            accentColor = accentColor,
            heroImageUrl = heroImageUrl,
            tag = tagValue ?: obj.optString("category"),
            isFeatured = obj.optBoolean("isFeatured", false),
            content = obj.optJSONArray("content").toStringList()
        )
    }

    private fun defaultCategories(): List<NewsCategory> = listOf(
        NewsCategory(0, "All"),
        NewsCategory(1, "Top"),
        NewsCategory(2, "Sport"),
        NewsCategory(3, "Business"),
        NewsCategory(4, "Entertainment"),
        NewsCategory(5, "Tech"),
        NewsCategory(6, "Health")
    )
}

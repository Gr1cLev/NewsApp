package com.example.newsapp.model

data class NewsCategory(
    val id: Int,
    val name: String
)

data class NewsArticle(
    val id: Int,
    val category: String,
    val title: String,
    val summary: String,
    val source: String,
    val publishedAt: String,
    val accentColor: Int,
    val heroImageUrl: String? = null,
    val tag: String = category,
    val isFeatured: Boolean = false,
    val content: List<String> = emptyList()
)

data class NewsData(
    val categories: List<NewsCategory>,
    val featuredArticles: List<NewsArticle>,
    val articles: List<NewsArticle>,
    val bookmarkedArticles: List<NewsArticle>,
    val searchSuggestions: List<String>
)

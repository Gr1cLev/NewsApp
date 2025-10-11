package com.example.newsapp.model

import androidx.compose.ui.graphics.Color
import com.example.newsapp.ui.theme.PrimaryBlue

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
    val accentColor: Color,
    val heroImageUrl: String? = null,
    val tag: String = category,
    val isFeatured: Boolean = false
)

val sampleCategories = listOf(
    NewsCategory(0, "All"),
    NewsCategory(1, "Top"),
    NewsCategory(2, "Sport"),
    NewsCategory(3, "Business"),
    NewsCategory(4, "Entertainment"),
    NewsCategory(5, "Tech"),
    NewsCategory(6, "Health")
)

val sampleFeaturedArticles = listOf(
    NewsArticle(
        id = 101,
        category = "Entertainment",
        title = "Horoscopes Sept. 6, 2023: Rosie Perez says knowledge will broaden your world",
        summary = "Discover what the stars have in store and why keeping an open mind sparks new opportunities.",
        source = "DailyStars",
        publishedAt = "2023-09-06 10:11:29",
        accentColor = PrimaryBlue,
        heroImageUrl = null,
        tag = "entertainment",
        isFeatured = true
    )
)

val sampleArticles = listOf(
    NewsArticle(
        id = 201,
        category = "Sport",
        title = "Kickoff | Pitso 'annoyed' at Sirino, Al Wahda links",
        summary = "The coach responds to rumours linking his star midfielder away from the club.",
        source = "snl24",
        publishedAt = "2023-09-06 10:11:29",
        accentColor = Color(0xFF0EA5E9)
    ),
    NewsArticle(
        id = 202,
        category = "Sport",
        title = "Kickoff | Broos revels in Foster and Lepasa scoring form",
        summary = "South Africa's attack clicks into gear with a newfound fluency and relentless press.",
        source = "snl24",
        publishedAt = "2023-09-06 10:11:29",
        accentColor = Color(0xFF14B8A6)
    ),
    NewsArticle(
        id = 203,
        category = "Entertainment",
        title = "Horoscopes Sept. 6, 2023: Rosie Perez knows knowledge broadens horizons",
        summary = "A light-hearted take on daily insights with tips on navigating the week ahead.",
        source = "mercurynews",
        publishedAt = "2023-09-06 10:10:11",
        accentColor = Color(0xFFF97316)
    ),
    NewsArticle(
        id = 204,
        category = "Top",
        title = "Dens Park smoke bomb kick has cost Leigh Griffiths GBP 17k",
        summary = "The forward faces a heavy sanction following last season's controversial incident.",
        source = "thecourier",
        publishedAt = "2023-09-06 09:56:13",
        accentColor = Color(0xFF6366F1)
    ),
    NewsArticle(
        id = 205,
        category = "Business",
        title = "Global markets steady as investors eye key inflation data",
        summary = "Analysts predict a cautious rally amid signals of cooling price pressure worldwide.",
        source = "finledger",
        publishedAt = "2023-09-06 09:30:00",
        accentColor = Color(0xFFD946EF)
    ),
    NewsArticle(
        id = 206,
        category = "Tech",
        title = "Startup unveils battery breakthrough promising week-long phone power",
        summary = "A novel solid-state design could extend mobile endurance without sacrificing size.",
        source = "techpulse",
        publishedAt = "2023-09-06 08:42:17",
        accentColor = Color(0xFF8B5CF6)
    )
)

val sampleBookmarks = listOf(
    sampleArticles[2],
    sampleArticles[4]
)

val sampleSearchSuggestions = listOf(
    "UEFA Champions League schedule",
    "Stock market open today",
    "Rosie Perez horoscope recap",
    "Local weather updates",
    "AI tools for students"
)



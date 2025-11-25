package com.example.newsapp.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Objects (DTO) untuk News API Response
 * Menggunakan Moshi untuk JSON parsing
 */

@JsonClass(generateAdapter = true)
data class NewsApiResponse(
    @Json(name = "status") val status: String,
    @Json(name = "totalResults") val totalResults: Int,
    @Json(name = "articles") val articles: List<ArticleDto>
)

@JsonClass(generateAdapter = true)
data class ArticleDto(
    @Json(name = "source") val source: SourceDto,
    @Json(name = "author") val author: String?,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "url") val url: String,
    @Json(name = "urlToImage") val urlToImage: String?,
    @Json(name = "publishedAt") val publishedAt: String,
    @Json(name = "content") val content: String?
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String
)

package com.example.newsapp.network.mapper

import android.graphics.Color
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.network.dto.ArticleDto
import kotlin.random.Random

/**
 * Mapper untuk convert API response (DTO) ke domain model
 */
object NewsMapper {
    
    /**
     * Convert ArticleDto dari API ke NewsArticle domain model
     * isFeatured akan di-set true untuk 5 artikel pertama saat mapping
     */
    fun ArticleDto.toNewsArticle(id: Int, category: String = "Umum", isFeatured: Boolean = false): NewsArticle {
        val description = this.description ?: this.content?.take(200) ?: "Tidak ada deskripsi"
        val fullContent = this.content ?: description
        
        // Split content into paragraphs (setiap ~300 karakter atau gunakan description saja)
        val contentParagraphs = if (fullContent.length > 300) {
            fullContent.chunked(300)
        } else {
            listOf(fullContent)
        }
        
        return NewsArticle(
            id = id,
            category = category,
            title = this.title,
            summary = description,
            source = this.source.name,
            publishedAt = formatPublishedDate(this.publishedAt),
            accentColor = generateRandomAccentColor(), // Random untuk variasi
            heroImageUrl = this.urlToImage ?: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=1200&q=80", // Fallback image
            tag = category.lowercase(),
            isFeatured = isFeatured,
            content = contentParagraphs
        )
    }
    
    /**
     * Format tanggal dari ISO 8601 ke format yang lebih readable
     * Input: "2024-01-15T10:30:00Z"
     * Output: "15 Jan 2024"
     */
    private fun formatPublishedDate(isoDate: String): String {
        return try {
            // Simple formatting - bisa diganti dengan SimpleDateFormat untuk format yang lebih baik
            val parts = isoDate.split("T")[0].split("-")
            val year = parts[0]
            val month = getMonthName(parts[1].toInt())
            val day = parts[2]
            "$day $month $year"
        } catch (e: Exception) {
            isoDate
        }
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "Mei"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Agu"
            9 -> "Sep"
            10 -> "Okt"
            11 -> "Nov"
            12 -> "Des"
            else -> ""
        }
    }
    
    /**
     * Generate random accent color untuk variasi visual
     */
    private fun generateRandomAccentColor(): Int {
        val colors = listOf(
            "#FF6B6B", // Red
            "#4ECDC4", // Teal
            "#45B7D1", // Blue
            "#FFA07A", // Orange
            "#98D8C8", // Green
            "#A8E6CF", // Light Green
            "#F7DC6F", // Yellow
            "#BB8FCE", // Purple
            "#85C1E2", // Sky Blue
            "#F8B500"  // Gold
        )
        return Color.parseColor(colors.random())
    }
    
    /**
     * Generate accent color berdasarkan kategori (backup method)
     */
    private fun generateAccentColor(category: String): Int {
        return when (category.lowercase()) {
            "sports" -> Color.parseColor("#FF6B6B")
            "business" -> Color.parseColor("#4ECDC4")
            "technology" -> Color.parseColor("#45B7D1")
            "entertainment" -> Color.parseColor("#FFA07A")
            "health" -> Color.parseColor("#98D8C8")
            "science" -> Color.parseColor("#A8E6CF")
            else -> Color.parseColor("#00AEEF")
        }
    }
    
    /**
     * Convert category name ke format yang sesuai dengan API
     */
    fun mapUiCategoryToApiCategory(uiCategory: String): String? {
        return when (uiCategory.lowercase()) {
            "all", "top" -> null // null = all categories
            "sports" -> "sports"
            "business" -> "business"
            "entertainment" -> "entertainment"
            "technology" -> "technology"
            "health" -> "health"
            "science" -> "science"
            else -> uiCategory.lowercase()
        }
    }
}

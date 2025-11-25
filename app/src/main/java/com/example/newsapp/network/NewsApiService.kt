package com.example.newsapp.network

import com.example.newsapp.network.dto.NewsApiResponse
import com.example.newsapp.network.dto.ArticleDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * News API Service untuk integrasi dengan NewsAPI.org
 * 
 * Dokumentasi API: https://newsapi.org/docs/endpoints
 * 
 * CARA SETUP:
 * 1. Daftar di https://newsapi.org/register untuk mendapatkan API key gratis
 * 2. Setelah dapat API key, buat file `local.properties` di root project
 * 3. Tambahkan baris: NEWS_API_KEY=your_api_key_here
 * 4. File local.properties sudah otomatis di .gitignore, jadi aman dari git
 * 
 * Contoh local.properties:
 * ```
 * sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
 * NEWS_API_KEY=abc123def456ghi789jkl
 * ```
 */
interface NewsApiService {
    
    /**
     * Get top headlines
     * Endpoint: /v2/top-headlines
     * 
     * @param country Kode negara (id untuk Indonesia)
     * @param category Kategori berita (business, technology, sports, dll)
     * @param query Kata kunci pencarian
     * @param pageSize Jumlah artikel per halaman (max 100)
     * @param page Nomor halaman
     */
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "id",
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): Response<NewsApiResponse>
    
    /**
     * Get everything (all articles)
     * Endpoint: /v2/everything
     * 
     * @param query Kata kunci pencarian (required)
     * @param sources Sumber berita (comma-separated)
     * @param domains Domain website (comma-separated)
     * @param from Tanggal mulai (ISO 8601 format: 2024-01-01)
     * @param to Tanggal akhir (ISO 8601 format: 2024-01-31)
     * @param language Kode bahasa (id untuk Indonesia)
     * @param sortBy Urutan (relevancy, popularity, publishedAt)
     * @param pageSize Jumlah artikel per halaman (max 100)
     * @param page Nomor halaman
     */
    @GET("v2/everything")
    suspend fun searchEverything(
        @Query("q") query: String,
        @Query("sources") sources: String? = null,
        @Query("domains") domains: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("language") language: String = "id",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): Response<NewsApiResponse>
    
    /**
     * Get news sources
     * Endpoint: /v2/top-headlines/sources
     */
    @GET("v2/top-headlines/sources")
    suspend fun getSources(
        @Query("country") country: String = "id",
        @Query("category") category: String? = null,
        @Query("language") language: String = "id"
    ): Response<NewsApiResponse>
}

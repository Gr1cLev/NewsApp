package com.example.newsapp.di

import android.content.Context
import android.util.Log
import com.example.newsapp.BuildConfig
import com.example.newsapp.network.NewsApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val TAG = "NetworkModule"
    private const val BASE_URL = "https://newsapi.org/"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    
    /**
     * IMPORTANT: API Key Management
     * 
     * CARA MENAMBAHKAN API KEY:
     * 1. Buka file `local.properties` di root project (sejajar dengan settings.gradle.kts)
     * 2. Tambahkan baris: NEWS_API_KEY=your_api_key_here
     * 3. Save file
     * 
     * Contoh local.properties:
     * ```
     * sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
     * NEWS_API_KEY=abc123def456ghi789jkl
     * ```
     * 
     * File local.properties sudah otomatis di .gitignore, jadi AMAN dari push ke GitHub!
     * 
     * Untuk development, Anda bisa temporary hardcode di bawah ini,
     * tapi JANGAN lupa hapus sebelum push ke GitHub!
     */
    private fun getApiKey(context: Context): String {
        // Priority 1: Ambil dari BuildConfig (hasil baca local.properties)
        val apiKey = BuildConfig.NEWS_API_KEY
        
        Log.d(TAG, "🔑 API Key from BuildConfig: ${if (apiKey.isNotBlank()) "***${apiKey.takeLast(4)}" else "EMPTY"}")
        
        if (apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE") {
            Log.d(TAG, "✅ Using API key from BuildConfig")
            return apiKey
        }
        
        // Priority 2: Temporary hardcode untuk testing (HARUS DIHAPUS!)
        // return "your_temporary_api_key_here"
        
        // Priority 3: Fallback ke demo mode
        Log.w(TAG, "⚠️ No API key found! Using demo mode")
        return "demo_mode" // Akan menggunakan data lokal saja
    }
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiKeyInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            val apiKey = getApiKey(context)
            val original = chain.request()
            
            Log.d(TAG, "🌐 API Request: ${original.url}")
            
            // Jika demo mode, skip request
            if (apiKey == "demo_mode") {
                Log.w(TAG, "⚠️ Demo mode active, skipping API call")
                return@Interceptor chain.proceed(original)
            }
            
            // Tambahkan API key ke request
            val url = original.url.newBuilder()
                .addQueryParameter("apiKey", apiKey)
                .build()
            
            Log.d(TAG, "🔗 Full URL: $url")
            
            val request = original.newBuilder()
                .url(url)
                .build()
            
            chain.proceed(request)
        }
    }
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService {
        return retrofit.create(NewsApiService::class.java)
    }
}

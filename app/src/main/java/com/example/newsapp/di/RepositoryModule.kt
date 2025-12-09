package com.example.newsapp.di

import android.content.Context
import com.example.newsapp.data.BookmarkRepository
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.firebase.FirebaseArticleCacheRepository
import com.example.newsapp.data.firebase.FirebaseBookmarkRepository
import com.example.newsapp.network.NewsApiService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideNewsRepository(
        @ApplicationContext context: Context,
        newsApiService: NewsApiService,
        firebaseBookmarkRepository: FirebaseBookmarkRepository,
        firebaseArticleCacheRepository: FirebaseArticleCacheRepository
    ): NewsRepository {
        return NewsRepository(context, newsApiService, firebaseBookmarkRepository, firebaseArticleCacheRepository)
    }
    
    @Provides
    @Singleton
    fun provideFirebaseArticleCacheRepository(
        firestore: FirebaseFirestore
    ): FirebaseArticleCacheRepository {
        return FirebaseArticleCacheRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideBookmarkRepository(): BookmarkRepository {
        return BookmarkRepository
    }
    
    @Provides
    @Singleton
    fun provideProfileRepository(): ProfileRepository {
        return ProfileRepository
    }
}

package com.example.newsapp.di

import android.content.Context
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.data.firebase.UserInteractionRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Firebase dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        
        // Configure Firestore settings with LIMITED cache to reduce memory pressure
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(50 * 1024 * 1024) // 50 MB cache (was unlimited)
            .build()
        
        firestore.firestoreSettings = settings
        
        return firestore
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            android.util.Log.d("FirebaseModule", "Initializing Firebase...")
            FirebaseApp.initializeApp(context)
        }
        
        val analytics = FirebaseAnalytics.getInstance(context)
        
        // Enable analytics collection explicitly
        analytics.setAnalyticsCollectionEnabled(true)
        
        android.util.Log.d("FirebaseModule", "Firebase Analytics initialized and enabled")
        
        return analytics
    }
    
    // ❌ Firebase Storage removed - ML model now stored in Firestore (FREE!)
    // No longer needed: provideFirebaseStorage()
}

/**
 * EntryPoint for accessing Firebase dependencies from non-Hilt contexts (e.g., Composables)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirebaseEntryPoint {
    fun firebaseAuthRepository(): FirebaseAuthRepository
    fun userInteractionRepository(): UserInteractionRepository
}

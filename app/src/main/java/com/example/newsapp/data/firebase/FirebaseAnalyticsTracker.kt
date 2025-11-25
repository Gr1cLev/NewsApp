package com.example.newsapp.data.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics wrapper for tracking events
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    
    companion object {
        // Event names
        const val EVENT_ARTICLE_CLICKED = "article_clicked"
        const val EVENT_ARTICLE_BOOKMARKED = "article_bookmarked"
        const val EVENT_ARTICLE_SHARED = "article_shared"
        const val EVENT_ARTICLE_READ_COMPLETE = "article_read_complete"
        const val EVENT_SEARCH_PERFORMED = "search_performed"
        const val EVENT_CATEGORY_SELECTED = "category_selected"
        const val EVENT_LOGIN = "user_login"
        const val EVENT_SIGNUP = "user_signup"
        const val EVENT_LOGOUT = "user_logout"
        
        // Parameter names
        const val PARAM_ARTICLE_ID = "article_id"
        const val PARAM_ARTICLE_TITLE = "article_title"
        const val PARAM_CATEGORY = "category"
        const val PARAM_SOURCE = "source"
        const val PARAM_SEARCH_QUERY = "search_query"
        const val PARAM_READING_TIME = "reading_time_seconds"
        const val PARAM_AUTH_METHOD = "auth_method"
    }
    
    /**
     * Track article click
     */
    fun trackArticleClick(
        articleId: String,
        title: String,
        category: String,
        source: String
    ) {
        val bundle = Bundle().apply {
            putString(PARAM_ARTICLE_ID, articleId)
            putString(PARAM_ARTICLE_TITLE, title)
            putString(PARAM_CATEGORY, category)
            putString(PARAM_SOURCE, source)
        }
        analytics.logEvent(EVENT_ARTICLE_CLICKED, bundle)
    }
    
    /**
     * Track article bookmark
     */
    fun trackArticleBookmark(
        articleId: String,
        title: String,
        category: String,
        isBookmarked: Boolean
    ) {
        val bundle = Bundle().apply {
            putString(PARAM_ARTICLE_ID, articleId)
            putString(PARAM_ARTICLE_TITLE, title)
            putString(PARAM_CATEGORY, category)
            putBoolean("is_bookmarked", isBookmarked)
        }
        analytics.logEvent(EVENT_ARTICLE_BOOKMARKED, bundle)
    }
    
    /**
     * Track article share
     */
    fun trackArticleShare(
        articleId: String,
        title: String,
        category: String
    ) {
        val bundle = Bundle().apply {
            putString(PARAM_ARTICLE_ID, articleId)
            putString(PARAM_ARTICLE_TITLE, title)
            putString(PARAM_CATEGORY, category)
        }
        analytics.logEvent(EVENT_ARTICLE_SHARED, bundle)
    }
    
    /**
     * Track article reading completion
     */
    fun trackArticleReadComplete(
        articleId: String,
        readingTimeSeconds: Long
    ) {
        val bundle = Bundle().apply {
            putString(PARAM_ARTICLE_ID, articleId)
            putLong(PARAM_READING_TIME, readingTimeSeconds)
        }
        analytics.logEvent(EVENT_ARTICLE_READ_COMPLETE, bundle)
    }
    
    /**
     * Track search
     */
    fun trackSearch(query: String, resultsCount: Int) {
        val bundle = Bundle().apply {
            putString(PARAM_SEARCH_QUERY, query)
            putInt("results_count", resultsCount)
        }
        analytics.logEvent(EVENT_SEARCH_PERFORMED, bundle)
    }
    
    /**
     * Track category selection
     */
    fun trackCategorySelected(category: String) {
        val bundle = Bundle().apply {
            putString(PARAM_CATEGORY, category)
        }
        analytics.logEvent(EVENT_CATEGORY_SELECTED, bundle)
    }
    
    /**
     * Track user login
     */
    fun trackLogin(method: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AUTH_METHOD, method)
        }
        analytics.logEvent(EVENT_LOGIN, bundle)
    }
    
    /**
     * Track user signup
     */
    fun trackSignup(method: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AUTH_METHOD, method)
        }
        analytics.logEvent(EVENT_SIGNUP, bundle)
    }
    
    /**
     * Track user logout
     */
    fun trackLogout() {
        analytics.logEvent(EVENT_LOGOUT, null)
    }
    
    /**
     * Set user ID for analytics
     */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }
    
    /**
     * Set user property
     */
    fun setUserProperty(name: String, value: String) {
        analytics.setUserProperty(name, value)
    }
    
    /**
     * Track screen view
     */
    fun trackScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}

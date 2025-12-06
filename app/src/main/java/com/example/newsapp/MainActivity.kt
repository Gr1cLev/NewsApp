package com.example.newsapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.ui.compose.NewsApp
import com.example.newsapp.ui.theme.NewsAppTheme
import com.example.newsapp.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var newsRepository: NewsRepository
    
    @Inject
    lateinit var firebaseAuthRepository: FirebaseAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val nightMode = if (UserPreferences.isNightModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        super.onCreate(savedInstanceState)
        newsRepository.invalidateCache()
        
        // Log current Firebase Auth state (do NOT override existing auth)
        lifecycleScope.launch {
            val currentUser = firebaseAuthRepository.getCurrentUser()
            if (currentUser != null) {
                val authMethod = if (currentUser.isAnonymous) "Anonymous" else "Authenticated"
                Log.d("MainActivity", "✅ Firebase User ($authMethod): ${currentUser.uid}")
            } else {
                Log.d("MainActivity", "⚠️ No Firebase user logged in")
            }
        }
        
        // DEBUG: Test API call
        lifecycleScope.launch {
            Log.d("MainActivity", "🔍 Testing API call...")
            try {
                val result = newsRepository.fetchArticlesFromNetwork(country = "id")
                when (result) {
                    is Resource.Success -> {
                        Log.d("MainActivity", "✅ API SUCCESS: ${result.data.size} articles")
                        result.data.take(3).forEach { article ->
                            Log.d("MainActivity", "  📰 ${article.title}")
                            Log.d("MainActivity", "     Source: ${article.source}")
                            Log.d("MainActivity", "     Featured: ${article.isFeatured}")
                        }
                    }
                    is Resource.Error -> {
                        Log.e("MainActivity", "❌ API ERROR: ${result.message}")
                    }
                    is Resource.Loading -> {
                        Log.d("MainActivity", "⏳ Loading...")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 EXCEPTION: ${e.message}", e)
            }
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(UserPreferences.isNightModeEnabled(this)) }
            NewsAppTheme(darkTheme = isDarkTheme) {
                NewsApp(
                    newsRepository = newsRepository,
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = { enabled ->
                        isDarkTheme = enabled
                    }
                )
            }
        }
    }
}

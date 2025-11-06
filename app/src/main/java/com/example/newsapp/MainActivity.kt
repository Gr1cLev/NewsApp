package com.example.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.ui.compose.NewsApp
import com.example.newsapp.ui.theme.NewsAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val nightMode = if (UserPreferences.isNightModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        super.onCreate(savedInstanceState)
        NewsRepository.invalidateCache()

        setContent {
            var isDarkTheme by remember { mutableStateOf(UserPreferences.isNightModeEnabled(this)) }
            NewsAppTheme(darkTheme = isDarkTheme) {
                NewsApp(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = { enabled ->
                        isDarkTheme = enabled
                    }
                )
            }
        }
    }
}

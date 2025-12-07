package com.example.newsapp.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppDestination {
    const val Login = "auth/login"
    const val Register = "auth/register"
    const val Home = "home"
    const val ArticleDetail = "article/{articleId}"
    const val Settings = "settings"
    const val EditProfile = "settings/edit-profile"
    const val BackgroundSettings = "settings/background"

    fun articleDetail(articleId: Int) = "article/$articleId"
}

@Composable
fun NewsApp(
    newsRepository: NewsRepository,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }
    var bookmarksVersion by remember { mutableIntStateOf(0) }
    var profileVersion by remember { mutableIntStateOf(0) }

    // Get Firebase Auth Repository
    val firebaseAuthRepository = remember {
        val appContext = context.applicationContext
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            appContext,
            com.example.newsapp.di.FirebaseEntryPoint::class.java
        ).firebaseAuthRepository()
    }
    
    LaunchedEffect(Unit) {
        // Check Firebase Auth state (supports Email, Google, and Anonymous)
        val isAuthenticated = firebaseAuthRepository.isUserAuthenticated()
        startDestination = if (isAuthenticated) {
            AppDestination.Home
        } else {
            AppDestination.Login
        }
    }

    val navController = rememberNavController()

    val resolvedStartDestination = startDestination
    if (resolvedStartDestination == null) {
        SplashScreen()
        return
    }

    NavHost(
        navController = navController,
        startDestination = resolvedStartDestination
    ) {
        composable(AppDestination.Login) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(AppDestination.Register)
                },
                onAuthenticated = {
                    newsRepository.invalidateCache()
                    bookmarksVersion++
                    profileVersion++
                    navController.navigate(AppDestination.Home) {
                        popUpTo(AppDestination.Login) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppDestination.Register) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegistered = {
                    newsRepository.invalidateCache()
                    bookmarksVersion++
                    profileVersion++
                    navController.navigate(AppDestination.Home) {
                        popUpTo(AppDestination.Login) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppDestination.Home) {
            HomeScreen(
                newsRepository = newsRepository,
                bookmarksVersion = bookmarksVersion,
                profileVersion = profileVersion,
                onOpenArticle = { articleId ->
                    navController.navigate(AppDestination.articleDetail(articleId))
                },
                onOpenSettings = {
                    navController.navigate(AppDestination.Settings)
                },
                onRequireAuthentication = {
                    navController.navigate(AppDestination.Login) {
                        popUpTo(AppDestination.Home) { inclusive = true }
                    }
                },
                onBookmarksUpdated = {
                    bookmarksVersion++
                }
            )
        }

        composable(
            route = AppDestination.ArticleDetail,
            arguments = listOf(
                navArgument("articleId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getInt("articleId") ?: return@composable
            ArticleDetailScreen(
                articleId = articleId,
                newsRepository = newsRepository,
                onBack = { navController.popBackStack() },
                onBookmarkChanged = {
                    bookmarksVersion++
                }
            )
        }

        composable(AppDestination.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(AppDestination.EditProfile) },
                onOpenBackground = { navController.navigate(AppDestination.BackgroundSettings) },
                onLogout = {
                    newsRepository.invalidateCache()
                    bookmarksVersion++
                    profileVersion++
                    navController.navigate(AppDestination.Login) {
                        popUpTo(AppDestination.Home) { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onThemeChanged
            )
        }

        composable(AppDestination.EditProfile) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onProfileSaved = {
                    profileVersion++
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestination.BackgroundSettings) {
            BackgroundSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

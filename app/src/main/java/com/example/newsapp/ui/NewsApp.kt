package com.example.newsapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newsapp.model.sampleArticles
import com.example.newsapp.model.sampleBookmarks
import com.example.newsapp.model.sampleCategories
import com.example.newsapp.model.sampleFeaturedArticles
import com.example.newsapp.model.sampleSearchSuggestions
import com.example.newsapp.ui.screens.BookmarksScreen
import com.example.newsapp.ui.screens.NewsHomeScreen
import com.example.newsapp.ui.screens.ProfileScreen
import com.example.newsapp.ui.screens.SearchScreen

private data class BottomNavDestination(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

private val bottomDestinations = listOf(
    BottomNavDestination("news", Icons.Filled.Home, "News"),
    BottomNavDestination("search", Icons.Filled.Search, "Search"),
    BottomNavDestination("bookmarks", Icons.Filled.Bookmarks, "Bookmarks"),
    BottomNavDestination("profile", Icons.Filled.Person, "Profile")
)

@Composable
fun NewsApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NewsBottomBar(
                destinations = bottomDestinations,
                currentDestinationRoute = currentDestination?.route,
                onNavigateTo = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NewsNavGraph(
            modifier = Modifier.padding(innerPadding),
            paddingValues = innerPadding,
            navController = navController
        )
    }
}

@Composable
private fun NewsNavGraph(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = bottomDestinations.first().route,
        modifier = modifier
    ) {
        composable(bottomDestinations[0].route) {
            NewsHomeScreen(
                categories = sampleCategories,
                featuredArticles = sampleFeaturedArticles,
                allArticles = sampleArticles,
                contentPadding = paddingValues
            )
        }
        composable(bottomDestinations[1].route) {
            SearchScreen(
                suggestions = sampleSearchSuggestions,
                articles = sampleArticles
            )
        }
        composable(bottomDestinations[2].route) {
            BookmarksScreen(bookmarks = sampleBookmarks)
        }
        composable(bottomDestinations[3].route) {
            ProfileScreen(
                bookmarked = sampleBookmarks.size
            )
        }
    }
}

@Composable
private fun NewsBottomBar(
    destinations: List<BottomNavDestination>,
    currentDestinationRoute: String?,
    onNavigateTo: (String) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestinationRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateTo(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

package com.example.classcrush.presentation.screen

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("discover", "Discover", Icons.Default.Explore),
        BottomNavItem("matches", "Matches", Icons.Default.Chat),
        BottomNavItem("profile", "Profile", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "discover",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("discover") {
                DiscoverScreen()
            }
            composable("matches") {
                MatchesScreen(
                    onNavigateToChat = { matchId, user ->
                        try {
                            // Validate user data before navigation
                            if (user.id.isNotEmpty() && user.name.isNotEmpty()) {
                                val userJson = Gson().toJson(user)
                                val encodedUserJson = Uri.encode(userJson)
                                mainNavController.navigate("chat/$matchId/$encodedUserJson")
                            } else {
                                android.util.Log.e("MainScreen", "Invalid user data: id=${user.id}, name=${user.name}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainScreen", "Error navigating to chat: ${e.message}", e)
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateToLogin = {
                        mainNavController.navigate("login") {
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

package com.example.classcrush.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.classcrush.data.model.User
import com.example.classcrush.presentation.screen.*
import com.google.gson.Gson

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToHome = { navController.navigate("main") },
                onNavigateToOnboarding = { navController.navigate("onboarding") }
            )
        }

        composable("signup") {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToOnboarding = { navController.navigate("onboarding") }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            // Pass the main NavController to the MainScreen
            MainScreen(mainNavController = navController)
        }

        composable(
            "chat/{matchId}/{userJson}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("userJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userJson = backStackEntry.arguments?.getString("userJson") ?: ""
            val decodedUserJson = Uri.decode(userJson)
            val otherUser = Gson().fromJson(decodedUserJson, User::class.java)

            ChatScreen(
                matchId = matchId,
                otherUser = otherUser,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSafety = { user ->
                    val json = Gson().toJson(user)
                    navController.navigate("safety/$json")
                }
            )
        }

        composable(
            "safety/{userJson}",
            arguments = listOf(navArgument("userJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val userJson = backStackEntry.arguments?.getString("userJson") ?: ""
            val user = Gson().fromJson(userJson, User::class.java)
            SafetyScreen(
                user = user,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

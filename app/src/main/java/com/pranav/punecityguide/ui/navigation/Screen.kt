package com.pranav.punecityguide.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Discover : Screen("discover")
    data object Community : Screen("community")
    data object Saved : Screen("saved")
    data object Profile : Screen("profile")
    data object Chatbot : Screen("chatbot")
    data object DiscoverCategory : Screen("discoverCategory/{categoryName}") {
        fun createRoute(categoryName: String) = "discoverCategory/${Uri.encode(categoryName)}"
    }
    data object Detail : Screen("detail/{attractionId}") {
        fun createRoute(attractionId: Int) = "detail/$attractionId"
    }
    data object Search : Screen("search")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object ScanToPlan : Screen("scan_to_plan")
    data object SecretSpots : Screen("secret_spots")
    data object MyPlans : Screen("my_plans")
    data object Onboarding : Screen("onboarding")
    data object CreatePost : Screen("create_post")
    data object Lounge : Screen("lounge")
    data object Audit : Screen("audit")
}

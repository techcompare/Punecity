package com.pranav.punecityguide.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Compare : Screen("compare")
    data object Trips : Screen("trips")
    data object Community : Screen("community")
    data object Profile : Screen("profile")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Onboarding : Screen("onboarding")
    data object ExpenseTracker : Screen("expense_tracker")
}

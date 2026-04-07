package com.pranav.punecityguide

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import com.pranav.punecityguide.ui.components.AiChatOverlay
import com.pranav.punecityguide.ui.components.AppBottomNavBar
import com.pranav.punecityguide.ui.navigation.Screen
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.PreferenceManager
import com.pranav.punecityguide.ui.theme.PuneCityTheme
import com.pranav.punecityguide.ui.viewmodel.AuthViewModel
import com.pranav.punecityguide.ui.viewmodel.AuthViewModelFactory
import com.pranav.punecityguide.ui.screens.*
import com.pranav.punecityguide.ui.screens.BrandSplashScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        SupabaseClient.initialize(this)

        // Keep system splash visible while auth is loading
        var isAuthReady = false
        splashScreen.setKeepOnScreenCondition { !isAuthReady }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        setContent {
            PuneCityTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val database = PuneCityDatabase.getInstance(this@MainActivity)
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(application)
                    )
                    val authState by authViewModel.uiState.collectAsState()
                    val preferenceManager = remember { PreferenceManager(application) }
                    val onboardingCompleted by preferenceManager.onboardingCompleted.collectAsState(initial = null)

                    // Signal system splash to dismiss once auth resolves
                    if (!authState.isLoading && onboardingCompleted != null) {
                        isAuthReady = true
                    }

                    var showBrandSplash by remember { mutableStateOf(true) }

                    if (onboardingCompleted != null) {
                        if (showBrandSplash) {
                            BrandSplashScreen(onFinished = { showBrandSplash = false })
                        } else {
                            AppNavigation(
                                database = database, 
                                authViewModel = authViewModel,
                                preferenceManager = preferenceManager,
                                isOnboardingCompleted = onboardingCompleted!!
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    database: PuneCityDatabase, 
    authViewModel: AuthViewModel,
    preferenceManager: PreferenceManager,
    isOnboardingCompleted: Boolean
) {
    val navController = rememberNavController()
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val healthService = remember { com.pranav.punecityguide.data.service.BackendHealthService(auditRepository) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabRoutes = setOf(
        Screen.Discover.route,
        Screen.Community.route,
        Screen.Saved.route,
        Screen.Profile.route
    )

    val authState by authViewModel.uiState.collectAsState()

    var showAiOverlay by remember { mutableStateOf(false) }
    var pendingAiMessage by remember { mutableStateOf("") }

    // Unified Launch Control
    val startDestination = remember(authState.isLoggedIn, isOnboardingCompleted) { 
        if (!isOnboardingCompleted) Screen.Onboarding.route
        else if (!authState.isLoggedIn) Screen.Login.route
        else Screen.Discover.route
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                AppBottomNavBar(
                    selectedRoute = currentRoute.orEmpty(),
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Discover.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(300)) + androidx.compose.animation.slideInHorizontally(tween(300)) { it / 4 } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) + androidx.compose.animation.slideInHorizontally(tween(300)) { -it / 4 } },
                popExitTransition = { fadeOut(tween(200)) + androidx.compose.animation.slideOutHorizontally(tween(200)) { it / 4 } }
            ) {
                composable(Screen.Onboarding.route) {
                    val scope = rememberCoroutineScope()
                    OnboardingScreen(
                        onFinished = {
                            scope.launch {
                                preferenceManager.setOnboardingCompleted(true)
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                        onLoginSuccess = { 
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onSkipAuth = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Signup.route) {
                    SignupScreen(
                        onNavigateToLogin = { navController.navigateUp() },
                        onSignupSuccess = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        },
                        onSkipAuth = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Discover.route) {
                    HomeScreen(
                        database = database,
                        healthService = healthService,
                        onNavigateToCategory = { category -> navController.navigate(Screen.DiscoverCategory.createRoute(category)) },
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToScan = { navController.navigate(Screen.ScanToPlan.route) },
                        onNavigateToSecret = { navController.navigate(Screen.SecretSpots.route) },
                        onNavigateToPlans = { navController.navigate(Screen.PlansSystem.route) },
                        onAskAi = { prompt ->
                            pendingAiMessage = prompt
                            showAiOverlay = true
                        }
                    )
                }

                composable(
                    "connect_post/{postId}",
                    arguments = listOf(navArgument("postId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId") ?: ""
                    ConnectPostDetailScreen(
                        postId = postId,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.Community.route) {
                    ConnectHomeScreen(
                        onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                        onNavigateToDetail = { postId -> navController.navigate("connect_post/$postId") },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onAiPrompt = { prompt ->
                            pendingAiMessage = prompt
                            showAiOverlay = true
                        }
                    )
                }

                composable(Screen.Saved.route) {
                    ConnectSavedScreen(
                        onNavigateToDetail = { postId -> navController.navigate("connect_post/$postId") }
                    )
                }

                composable(Screen.Lounge.route) {
                    CityLoungeScreen()
                }

                composable(Screen.Profile.route) {
                    ConnectProfileScreen(
                        database = database,
                        onNavigateToSaved = { navController.navigate(Screen.Saved.route) },
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onSignOut = {
                            authViewModel.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToAudit = { navController.navigate(Screen.Audit.route) }
                    )
                }

                composable(
                    Screen.CreatePost.route,
                    arguments = listOf(
                        navArgument("text") { type = NavType.StringType; defaultValue = ""; nullable = true },
                        navArgument("area") { type = NavType.StringType; defaultValue = ""; nullable = true }
                    )
                ) { backStackEntry ->
                    val text = backStackEntry.arguments?.getString("text") ?: ""
                    val area = backStackEntry.arguments?.getString("area") ?: ""
                    ConnectCreatePostScreen(
                        onNavigateBack = { navController.navigateUp() },
                        onPostCreated = { navController.navigateUp() },
                        initialDescription = text,
                        initialArea = area
                    )
                }

                composable(
                    Screen.DiscoverCategory.route,
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = Uri.decode(backStackEntry.arguments?.getString("categoryName") ?: "Category")
                    DiscoverCategoryScreen(
                        category = categoryName,
                        database = database,
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(
                    Screen.Detail.route,
                    arguments = listOf(navArgument("attractionId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val attractionId = backStackEntry.arguments?.getInt("attractionId") ?: 0
                    DetailScreen(
                        attractionId = attractionId,
                        database = database,
                        onNavigateBack = { navController.navigateUp() },
                        onAskAi = { prompt ->
                            pendingAiMessage = prompt
                            showAiOverlay = true
                        },
                        onPostToCommunity = { text ->
                            navController.navigate(Screen.CreatePost.createRoute(text = text))
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        database = database,
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.Audit.route) {
                    AuditScreen(
                        healthService = healthService,
                        auditRepository = auditRepository,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.ScanToPlan.route) {
                    ScanToPlanScreen(
                        database = database,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.SecretSpots.route) {
                    SecretSpotsScreen(
                        database = database,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                    )
                }

                composable(Screen.MyPlans.route) {
                    MyPlansScreen(
                        database = database,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.PlansSystem.route) {
                    PlansSystemScreen(
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToDetail = { id -> navController.navigate(Screen.PlanDetail.createRoute(id)) }
                    )
                }

                composable(
                    Screen.PlanDetail.route,
                    arguments = listOf(navArgument("planId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val planId = backStackEntry.arguments?.getString("planId") ?: ""
                    PlanDetailScreen(
                        planId = planId,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }
            }

            AnimatedVisibility(
                visible = showAiOverlay,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AiChatOverlay(
                    database = database,
                    onClose = { showAiOverlay = false },
                    initialMessage = pendingAiMessage
                )
            }

            FloatingActionButton(
                onClick = { showAiOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = if (currentRoute in tabRoutes) 56.dp else 0.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    }
}

package com.pranav.punecityguide

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pranav.punecityguide.ui.screens.AuthScreen
import com.pranav.punecityguide.ui.screens.AuthViewModel
import com.pranav.punecityguide.ui.screens.CommunityScreen
import com.pranav.punecityguide.ui.screens.CommunityViewModel
import com.pranav.punecityguide.ui.screens.DiscoveryViewModel
import com.pranav.punecityguide.ui.screens.HomeScreen
import com.pranav.punecityguide.ui.screens.PrivacyPolicyScreen
import com.pranav.punecityguide.ui.screens.ProfileScreen
import com.pranav.punecityguide.ui.screens.ProfileViewModel
import com.pranav.punecityguide.ui.screens.SavedScreen
import com.pranav.punecityguide.ui.screens.SavedViewModel
import com.pranav.punecityguide.ui.screens.SpotDetailsScreen
import com.pranav.punecityguide.ui.screens.TermsOfServiceScreen
import com.pranav.punecityguide.ui.screens.WelcomeScreen
import com.pranav.punecityguide.ui.screens.PlansScreen
import com.pranav.punecityguide.ui.screens.PlansViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pranav.punecityguide.data.SavedRepository
import com.pranav.punecityguide.model.PuneSpot
import kotlinx.coroutines.launch
import com.pranav.punecityguide.ui.theme.BuzzBackgroundEnd
import com.pranav.punecityguide.ui.theme.BuzzBackgroundStart
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted


data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun PuneBuzzApp(
    showWelcomeOnStart: Boolean,
    onWelcomeCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences("punebuzz_prefs", Context.MODE_PRIVATE) }
    val navController = rememberNavController()
    var sessionMode by remember { mutableStateOf(appPrefs.getString("session_mode", "none") ?: "none") }
    var sessionName by remember { mutableStateOf(appPrefs.getString("session_name", "Pune User") ?: "Pune User") }
    var sessionEmail by remember {
        mutableStateOf(appPrefs.getString("session_email", null)?.takeIf { it.isNotBlank() })
    }
    var sessionToken by remember { mutableStateOf(appPrefs.getString("session_token", "") ?: "") }
    val hasSession = sessionMode != "none"
    val startRoute = if (showWelcomeOnStart) "welcome" else if (hasSession) "discover" else "auth"
    val items = listOf(
        BottomNavItem("discover", "Discover", Icons.Default.Explore),
        BottomNavItem("plans", "Plans", Icons.Default.Route),
        BottomNavItem("community", "Community", Icons.Default.Groups),
        BottomNavItem("saved", "Saved", Icons.Default.Bookmark),
        BottomNavItem("profile", "Profile", Icons.Default.Person),
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isMainScreen = items.any { it.route == currentDestination?.route }
            
            if (isMainScreen) {
                NavigationBar(
                    containerColor = BuzzCard.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 12.dp).clip(RoundedCornerShape(24.dp))
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { 
                                Icon(
                                    item.icon, 
                                    contentDescription = item.title,
                                    tint = if (selected) BuzzPrimary else BuzzTextMuted
                                ) 
                            },
                            label = { 
                                Text(
                                    item.title, 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) BuzzPrimary else BuzzTextMuted
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = BuzzPrimary.copy(alpha = 0.1f)
                            )

                        )
                    }
                }
            }
        },

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BuzzBackgroundStart, BuzzBackgroundEnd)))
                .padding(innerPadding),
        ) {
            NavHost(navController = navController, startDestination = startRoute) {
                composable("welcome") {
                    WelcomeScreen(
                        onGetStarted = {
                            onWelcomeCompleted()
                            navController.navigate(if (hasSession) "discover" else "auth") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        },
                    )
                }
                composable("auth") {
                    val authViewModel: AuthViewModel = viewModel()
                    val uiState by authViewModel.uiState.collectAsState()

                    LaunchedEffect(uiState.sessionEvent) {
                        val session = uiState.sessionEvent ?: return@LaunchedEffect
                        appPrefs.edit()
                            .putString("session_mode", session.mode)
                            .putString("session_name", session.displayName)
                            .putString("session_email", session.email ?: "")
                            .putString("session_token", session.accessToken ?: "")
                            .apply()
                        sessionMode = session.mode
                        sessionName = session.displayName
                        sessionEmail = session.email?.takeIf { it.isNotBlank() }
                        sessionToken = session.accessToken ?: ""
                        authViewModel.consumeSessionEvent()
                        navController.navigate("discover") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }

                    AuthScreen(
                        uiState = uiState,
                        onLogin = authViewModel::signIn,
                        onSignUp = { email, password, confirmPassword, fullName ->
                            authViewModel.signUp(email, password, confirmPassword, fullName)
                        },
                        onContinueAsGuest = authViewModel::continueAsGuest,
                    )
                }
                composable("discover") {
                    val discoveryViewModel: DiscoveryViewModel = viewModel()
                    val uiState by discoveryViewModel.uiState.collectAsState()
                    var selectedSpot by remember { mutableStateOf<PuneSpot?>(null) }
                    var saveEpoch by remember { mutableIntStateOf(0) }
                    val appCtx = context.applicationContext
                    val scope = rememberCoroutineScope()

                    if (selectedSpot != null) {
                        val spot = selectedSpot!!
                        var isSaved by remember(spot.id, saveEpoch) { mutableStateOf(false) }

                        LaunchedEffect(spot.id, saveEpoch) {
                            isSaved = SavedRepository.isSaved(appCtx, spot.id)
                        }

                        SpotDetailsScreen(
                            spot = spot,
                            isSaved = isSaved,
                            onBack = { selectedSpot = null },
                            onToggleSave = {
                                scope.launch {
                                    SavedRepository.toggleSave(appCtx, spot)
                                    saveEpoch++
                                }
                            },
                            onOpenMap = { query ->
                                val q = Uri.encode(query)
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    Uri.parse("geo:0,0?q=$q"),
                                )
                                context.startActivity(intent)
                            },
                        )
                    } else {
                        HomeScreen(
                            spots = uiState.spots,
                            isLoading = uiState.isLoading,
                            currentCategory = uiState.currentCategory,
                            error = uiState.error,
                            onRetry = discoveryViewModel::refresh,
                            onCategorySelected = discoveryViewModel::setCategory,
                            onSpotSelected = { spot -> selectedSpot = spot }
                        )
                    }
                }
                composable("community") {
                    val communityViewModel: CommunityViewModel = viewModel()
                    val uiState by communityViewModel.uiState.collectAsState()
                    CommunityScreen(
                        currentUserName = sessionName,
                        canPost = sessionMode == "auth",
                        isLoading = uiState.isLoading,
                        isPosting = uiState.isPosting,
                        posts = uiState.posts,
                        error = uiState.error,
                        info = uiState.info,
                        onRefresh = communityViewModel::refresh,
                        onCreatePost = { content -> 
                            communityViewModel.createPost(
                                author = sessionName, 
                                content = content,
                                userToken = if (sessionMode == "auth") sessionToken else null
                            ) 
                        },
                        onUpdatePost = { postId, newContent ->
                            communityViewModel.updatePost(
                                postId = postId,
                                newContent = newContent,
                                userToken = if (sessionMode == "auth") sessionToken else null
                            )
                        },
                        onDeletePost = { postId ->
                            communityViewModel.deletePost(
                                postId = postId,
                                userToken = if (sessionMode == "auth") sessionToken else null
                            )
                        },
                        onOpenAuth = {
                            navController.navigate("auth")
                        },
                    )
                }
                composable("plans") {
                    val plansViewModel: PlansViewModel = viewModel()
                    val uiState by plansViewModel.uiState.collectAsState()
                    PlansScreen(
                        plans = uiState.plans,
                        isLoading = uiState.isLoading,
                        onPlanSelected = { plan ->
                            // Plan detail navigation (future enhancement)
                        }
                    )
                }
                composable("saved") {
                    val savedViewModel: SavedViewModel = viewModel()
                    val uiState by savedViewModel.uiState.collectAsState()
                    SavedScreen(
                        isLoading = uiState.isLoading,
                        places = uiState.places,
                        error = uiState.error,
                        onRetry = savedViewModel::refresh,
                    )
                }
                composable("profile") {
                    val profileViewModel: ProfileViewModel = viewModel()
                    val uiState by profileViewModel.uiState.collectAsState()
                    ProfileScreen(
                        isLoading = uiState.isLoading,
                        profile = uiState.profile,
                        error = uiState.error,
                        isGuest = sessionMode == "guest",
                        sessionDisplayName = sessionName,
                        sessionEmail = sessionEmail,
                        postsCount = uiState.postsCount,
                        savedCount = uiState.savedCount,
                        visitsCount = uiState.visitsCount,
                        xp = uiState.xp,
                        updateSuccess = uiState.updateSuccess,
                        onRetry = profileViewModel::refresh,
                        onLogout = {
                            appPrefs.edit()
                                .remove("session_mode")
                                .remove("session_name")
                                .remove("session_email")
                                .remove("session_token")
                                .remove("profile_photo_uri")
                                .apply()
                            sessionMode = "none"
                            sessionName = "Pune User"
                            sessionEmail = null
                            sessionToken = ""
                            navController.navigate("auth") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchToAuth = {
                            navController.navigate("auth")
                        },
                        onUpdateDisplayName = { newName ->
                            profileViewModel.updateDisplayName(newName)
                            // Also update session name
                            sessionName = newName
                        },
                        onUpdatePhoto = { uri ->
                            profileViewModel.updateProfilePhoto(uri)
                        },
                        onRemovePhoto = {
                            profileViewModel.removeProfilePhoto()
                        },
                        onClearUpdateSuccess = {
                            profileViewModel.clearUpdateSuccess()
                        },
                        onOpenPrivacyPolicy = {
                            navController.navigate("privacy_policy")
                        },
                        onOpenTerms = {
                            navController.navigate("terms")
                        }
                    )
                }
                composable("privacy_policy") {
                    PrivacyPolicyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("terms") {
                    TermsOfServiceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

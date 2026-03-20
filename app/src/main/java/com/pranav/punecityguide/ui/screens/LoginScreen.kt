package com.pranav.punecityguide.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.ui.viewmodel.AuthViewModel
import com.pranav.punecityguide.ui.viewmodel.AuthViewModelFactory

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    onSkipAuth: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { formVisible = true }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Immersive Background ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val brush = Brush.linearGradient(
                colors = listOf(Color(0xFF3A1857), Color(0xFF5B2A86), Color(0xFF8A5BB8)),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            drawRect(brush = brush)
        }

        // ── Decorative circles ──
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
            drawCircle(color = Color.White, radius = 350f, center = Offset(size.width * 0.85f, size.height * 0.15f))
            drawCircle(color = Color.White, radius = 250f, center = Offset(size.width * 0.1f, size.height * 0.85f))
            drawCircle(color = Color(0xFFFF7A1A), radius = 180f, center = Offset(size.width * 0.5f, size.height * 0.05f))
        }

        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(48.dp))

                // ── Brand Identity ──
                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Pune City Guide",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your AI-powered local companion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ── Glassmorphic Form ──
                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(800, delayMillis = 200))
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Welcome Back",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    if (!uiState.errorMessage.isNullOrEmpty()) viewModel.clearError()
                                },
                                placeholder = { Text("Email Address", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White.copy(alpha = 0.6f)) }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    if (!uiState.errorMessage.isNullOrEmpty()) viewModel.clearError()
                                },
                                placeholder = { Text("Password", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (email.trim().isNotEmpty() && password.isNotEmpty()) viewModel.signIn(email, password)
                                }),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.6f)) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide" else "Show",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            if (!uiState.errorMessage.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFF6B6B).copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                ) {
                                    Text(
                                        text = uiState.errorMessage!!,
                                        color = Color(0xFFFFB3B3),
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (email.trim().isNotEmpty() && password.isNotEmpty()) {
                                        viewModel.signIn(email, password)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF5B2A86)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color(0xFF5B2A86))
                                } else {
                                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // ── Divider ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
                                Text("or", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
                            }

                            Spacer(Modifier.height(12.dp))

                            // ── Create Account Button (prominent) ──
                            OutlinedButton(
                                onClick = onNavigateToSignup,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !uiState.isLoading
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Bottom actions ──
                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(600, delayMillis = 500))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = onSkipAuth) {
                            Text("Continue as Guest", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

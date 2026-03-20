package com.pranav.punecityguide.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    onSkipAuth: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(true) }
    var formVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { formVisible = true }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignupSuccess()
    }

    LaunchedEffect(uiState.awaitingEmailConfirmation) {
        if (uiState.awaitingEmailConfirmation) onNavigateToLogin()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Background ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val brush = Brush.linearGradient(
                colors = listOf(Color(0xFF5B2A86), Color(0xFF8A5BB8), Color(0xFFB388FF)),
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height)
            )
            drawRect(brush = brush)
        }

        Canvas(modifier = Modifier.fillMaxSize().alpha(0.12f)) {
            drawCircle(color = Color.White, radius = 300f, center = Offset(size.width * 0.9f, size.height * 0.2f))
            drawCircle(color = Color(0xFFFF7A1A), radius = 200f, center = Offset(size.width * 0.15f, size.height * 0.9f))
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
                Spacer(Modifier.height(36.dp))

                // ── Brand ──
                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Explore, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Join Pune City", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text("Create your explorer account", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Form ──
                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(800, delayMillis = 200))
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)), RoundedCornerShape(28.dp))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; if (!uiState.errorMessage.isNullOrEmpty()) viewModel.clearError() },
                                placeholder = { Text("Email Address", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White.copy(alpha = 0.6f)) }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordsMatch = confirmPassword == it || confirmPassword.isEmpty()
                                    if (!uiState.errorMessage.isNullOrEmpty()) viewModel.clearError()
                                },
                                placeholder = { Text("Password", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.6f)) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm Password
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    passwordsMatch = it == password || it.isEmpty()
                                    if (!uiState.errorMessage.isNullOrEmpty()) viewModel.clearError()
                                },
                                placeholder = { Text("Confirm Password", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (email.trim().isNotEmpty() && password.isNotEmpty() && passwordsMatch) viewModel.signUp(email, password)
                                }),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                isError = !passwordsMatch,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White,
                                    errorBorderColor = Color(0xFFFFB3B3)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.6f)) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                        Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            )

                            if (!passwordsMatch) {
                                Text("Passwords don't match", color = Color(0xFFFFB3B3), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                            }

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
                                    if (email.trim().isNotEmpty() && password.isNotEmpty() && passwordsMatch) {
                                        viewModel.signUp(email, password)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF5B2A86)),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank() && passwordsMatch
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color(0xFF5B2A86))
                                } else {
                                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(visible = formVisible, enter = fadeIn(tween(600, delayMillis = 500))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = onSkipAuth) {
                            Text("Skip for now", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Already a member?", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onNavigateToLogin) {
                                Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

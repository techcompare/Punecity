package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzBackgroundEnd
import com.pranav.punecityguide.ui.theme.BuzzBackgroundStart
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    var tabIndex by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    // Validation states
    val emailError by remember(email) {
        derivedStateOf {
            when {
                email.isBlank() -> null
                !email.contains("@") || !email.contains(".") -> "Enter a valid email"
                else -> null
            }
        }
    }
    
    val passwordError by remember(password) {
        derivedStateOf {
            when {
                password.isBlank() -> null
                password.length < 6 -> "Minimum 6 characters"
                else -> null
            }
        }
    }
    
    val confirmPasswordError by remember(password, confirmPassword) {
        derivedStateOf {
            when {
                confirmPassword.isBlank() -> null
                confirmPassword != password -> "Passwords do not match"
                else -> null
            }
        }
    }
    
    val nameError by remember(fullName) {
        derivedStateOf {
            when {
                fullName.isBlank() -> null
                fullName.length < 2 -> "Name too short"
                else -> null
            }
        }
    }
    
    // Form validity
    val isLoginValid by remember(email, password, emailError, passwordError) {
        derivedStateOf {
            email.isNotBlank() && password.isNotBlank() && 
            emailError == null && passwordError == null
        }
    }
    
    val isSignUpValid by remember(email, password, confirmPassword, fullName, emailError, passwordError, confirmPasswordError, nameError) {
        derivedStateOf {
            email.isNotBlank() && password.isNotBlank() && 
            confirmPassword.isNotBlank() && fullName.isNotBlank() &&
            emailError == null && passwordError == null && 
            confirmPasswordError == null && nameError == null
        }
    }
    
    // Password strength indicator
    val passwordStrength by remember(password) {
        derivedStateOf {
            when {
                password.isEmpty() -> 0
                password.length < 6 -> 1
                password.length < 8 -> 2
                password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 4
                password.any { it.isDigit() } || password.any { it.isUpperCase() } -> 3
                else -> 2
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BuzzBackgroundStart, BuzzBackgroundEnd)))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and tagline
            Text(
                "Pune Buzz",
                style = MaterialTheme.typography.headlineLarge,
                color = BuzzPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Discover the heart of Pune",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BuzzCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = Color.Transparent,
                        contentColor = BuzzPrimary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                    color = BuzzPrimary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = tabIndex == 0,
                            onClick = { tabIndex = 0 },
                            text = { 
                                Text(
                                    "Sign In", 
                                    fontWeight = if(tabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                        Tab(
                            selected = tabIndex == 1,
                            onClick = { tabIndex = 1 },
                            text = { 
                                Text(
                                    "Create Account", 
                                    fontWeight = if(tabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Name (Sign Up only)
                    AnimatedVisibility(
                        visible = tabIndex == 1,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Your name", color = BuzzTextMuted) },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Person, 
                                    contentDescription = null,
                                    tint = if (nameError == null && fullName.isNotBlank()) BuzzPrimary else BuzzTextMuted
                                ) 
                            },
                            trailingIcon = {
                                if (fullName.isNotBlank() && nameError == null) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80)
                                    )
                                }
                            },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it, color = BuzzSecondary) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BuzzPrimary,
                                unfocusedBorderColor = BuzzTextMuted.copy(alpha = 0.3f),
                                errorBorderColor = BuzzSecondary
                            )
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email") },
                        placeholder = { Text("your@email.com", color = BuzzTextMuted) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Email, 
                                contentDescription = null,
                                tint = if (emailError == null && email.isNotBlank()) BuzzPrimary else BuzzTextMuted
                            ) 
                        },
                        trailingIcon = {
                            if (email.isNotBlank() && emailError == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80)
                                )
                            }
                        },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it, color = BuzzSecondary) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuzzPrimary,
                            unfocusedBorderColor = BuzzTextMuted.copy(alpha = 0.3f),
                            errorBorderColor = BuzzSecondary
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("Minimum 6 characters", color = BuzzTextMuted) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Lock, 
                                contentDescription = null,
                                tint = if (passwordError == null && password.isNotBlank()) BuzzPrimary else BuzzTextMuted
                            ) 
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = BuzzTextMuted
                                )
                            }
                        },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it, color = BuzzSecondary) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (tabIndex == 0) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { 
                                if (tabIndex == 0 && isLoginValid) {
                                    onLogin(email, password) 
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuzzPrimary,
                            unfocusedBorderColor = BuzzTextMuted.copy(alpha = 0.3f),
                            errorBorderColor = BuzzSecondary
                        )
                    )
                    
                    // Password strength indicator (Sign Up only)
                    AnimatedVisibility(visible = tabIndex == 1 && password.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(4) { index ->
                                    val color by animateColorAsState(
                                        targetValue = when {
                                            index < passwordStrength -> when (passwordStrength) {
                                                1 -> BuzzSecondary
                                                2 -> Color(0xFFFBBF24)
                                                3 -> Color(0xFF4ADE80)
                                                4 -> BuzzPrimary
                                                else -> BuzzCard
                                            }
                                            else -> BuzzCard
                                        },
                                        label = "strengthColor"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(color)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (passwordStrength) {
                                    1 -> "Weak"
                                    2 -> "Fair"
                                    3 -> "Good"
                                    4 -> "Strong"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (passwordStrength) {
                                    1 -> BuzzSecondary
                                    2 -> Color(0xFFFBBF24)
                                    3 -> Color(0xFF4ADE80)
                                    4 -> BuzzPrimary
                                    else -> BuzzTextMuted
                                }
                            )
                        }
                    }

                    // Confirm Password (Sign Up only)
                    AnimatedVisibility(
                        visible = tabIndex == 1,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password", color = BuzzTextMuted) },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Lock, 
                                    contentDescription = null,
                                    tint = if (confirmPasswordError == null && confirmPassword.isNotBlank()) BuzzPrimary else BuzzTextMuted
                                ) 
                            },
                            trailingIcon = {
                                Row {
                                    if (confirmPassword.isNotBlank() && confirmPasswordError == null) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = BuzzTextMuted
                                        )
                                    }
                                }
                            },
                            isError = confirmPasswordError != null,
                            supportingText = confirmPasswordError?.let { { Text(it, color = BuzzSecondary) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { 
                                    if (isSignUpValid) {
                                        onSignUp(email, password, confirmPassword, fullName) 
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BuzzPrimary,
                                unfocusedBorderColor = BuzzTextMuted.copy(alpha = 0.3f),
                                errorBorderColor = BuzzSecondary
                            )
                        )
                    }

                    // Error message
                    AnimatedVisibility(visible = uiState.error != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BuzzSecondary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                uiState.error ?: "",
                                color = BuzzSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Success message
                    AnimatedVisibility(visible = uiState.info != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                uiState.info ?: "",
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (tabIndex == 0) onLogin(email, password)
                            else onSignUp(email, password, confirmPassword, fullName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading && (if (tabIndex == 0) isLoginValid else isSignUpValid),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BuzzPrimary,
                            disabledContainerColor = BuzzPrimary.copy(alpha = 0.4f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Please wait...")
                        } else {
                            Text(
                                if (tabIndex == 0) "Sign In" else "Create Account",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Guest option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(BuzzTextMuted.copy(alpha = 0.3f))
                    )
                    Text(
                        "  or  ",
                        color = BuzzTextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(BuzzTextMuted.copy(alpha = 0.3f))
                    )
                }
                
                TextButton(
                    onClick = onContinueAsGuest, 
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        "Continue as Guest", 
                        color = BuzzTextMuted, 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    "Guest users can explore but cannot post or save",
                    style = MaterialTheme.typography.labelSmall,
                    color = BuzzTextMuted.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "By continuing, you agree to our Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.labelSmall,
                    color = BuzzTextMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


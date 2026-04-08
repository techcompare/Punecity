package com.pranav.punecityguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pranav.punecityguide.ui.theme.PuneBuzzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appPrefs = getSharedPreferences("punebuzz_prefs", MODE_PRIVATE)
        val showWelcome = appPrefs.getBoolean("show_welcome", true)

        setContent {
            PuneBuzzTheme {
                PuneBuzzApp(
                    showWelcomeOnStart = showWelcome,
                    onWelcomeCompleted = {
                        appPrefs.edit().putBoolean("show_welcome", false).apply()
                    },
                )
            }
        }
    }
}

package com.example.smartalignora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartalignora.ui.screens.AboutScreen
import com.example.smartalignora.ui.screens.DeviceSetupScreen
import com.example.smartalignora.ui.screens.SplashScreen

object Routes {
    const val SPLASH       = "splash"
    const val ABOUT        = "about"
    const val DEVICE_SETUP = "device_setup"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Page 1 — Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        // Page 2 — About
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                },
                onGetStarted = {
                    navController.navigate(Routes.DEVICE_SETUP) // → Page 3
                }
            )
        }

        // Page 3 — Device Setup
        composable(Routes.DEVICE_SETUP) {
            DeviceSetupScreen(
                onNext = {
                    // Page 4 will go here later
                },
                onSkip = {
                    // Skip to main app later
                }
            )
        }
    }
}
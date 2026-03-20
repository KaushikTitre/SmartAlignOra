package com.example.smartalignora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartalignora.ui.screens.*

object Routes {
    const val SPLASH        = "splash"
    const val ABOUT         = "about"
    const val DEVICE_SETUP  = "device_setup"
    const val HOW_TO_WEAR   = "how_to_wear"
    const val CLIP_ON_GUIDE = "clip_on_guide"
    const val LOGIN         = "login"
    const val SIGN_UP       = "sign_up"
    const val HOME          = "home"
    const val ANALYSIS      = "analysis"
    const val SETTINGS      = "settings"
    const val PROFILE       = "profile"
    const val FALL_PLAYER   = "fall_player"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onGetStarted = { navController.navigate(Routes.DEVICE_SETUP) }
            )
        }

        composable(Routes.DEVICE_SETUP) {
            DeviceSetupScreen(
                onNext = { navController.navigate(Routes.HOW_TO_WEAR) },
                onSkip = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.HOW_TO_WEAR) {
            HowToWearScreen(
                onNext = { navController.navigate(Routes.CLIP_ON_GUIDE) },
                onSkip = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.CLIP_ON_GUIDE) {
            ClipOnGuideScreen(
                onFinish = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = { navController.navigate(Routes.HOME) },
                onSignUp = { navController.navigate(Routes.SIGN_UP) },
                onForgotPassword = { },
                onChatBot = { }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignUp = { navController.navigate(Routes.HOME) },
                onLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAnalysis   = { navController.navigate(Routes.ANALYSIS) },
                onNavigateToSettings   = { navController.navigate(Routes.SETTINGS) },
                onNavigateToProfile    = { navController.navigate(Routes.PROFILE) },
                onNavigateToFallPlayer = { navController.navigate(Routes.FALL_PLAYER) }
            )
        }

        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack   = { navController.popBackStack() },
                onLogout = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack               = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.FALL_PLAYER) {
            FallDataPlayerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
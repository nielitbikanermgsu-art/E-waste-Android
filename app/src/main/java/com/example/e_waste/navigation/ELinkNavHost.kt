package com.example.e_waste.navigation



import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elink.app.ui.capture.CaptureScreen
import com.elink.app.ui.handover.HandoverScreen
import com.elink.app.ui.history.HistoryScreen
import com.elink.app.ui.home.HomeScreen
import com.elink.app.ui.result.ResultScreen

/**
 * Top-level nav graph. Kept flat (no nested graphs) since the app only has five screens —
 * nesting would add ceremony without paying for itself at this size.
 */
@Composable
fun ElinkNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {

        composable(Destinations.HOME) {
            HomeScreen(
                onCaptureClick = { navController.navigate(Destinations.CAPTURE) },
                onHistoryClick = { navController.navigate(Destinations.HISTORY) },
                onLotClick = { sessionId ->
                    navController.navigate(Destinations.handoverRoute(sessionId))
                }
            )
        }

        composable(Destinations.CAPTURE) {
            CaptureScreen(
                onPhotoCaptured = { photoUri ->
                    // Pop Capture off the back stack so the user can't "go back" into the camera
                    // after a photo is taken — Result becomes the new back-stack head.
                    navController.navigate(Destinations.resultRoute(photoUri.toString())) {
                        popUpTo(Destinations.CAPTURE) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.RESULT_ROUTE,
            arguments = listOf(
                navArgument(Destinations.RESULT_ARG_PHOTO_URI) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val photoUri = backStackEntry.arguments
                ?.getString(Destinations.RESULT_ARG_PHOTO_URI)
                .orEmpty()
            ResultScreen(
                photoUri = photoUri,
                onConfirmHandover = { sessionId ->
                    navController.navigate(Destinations.handoverRoute(sessionId))
                },
                onRetry = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.HANDOVER_ROUTE,
            arguments = listOf(
                navArgument(Destinations.HANDOVER_ARG_SESSION_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments
                ?.getString(Destinations.HANDOVER_ARG_SESSION_ID)
                .orEmpty()
            HandoverScreen(
                sessionId = sessionId,
                onDone = {
                    // Clear back to Home once a lot is finalised — no reason to let the user
                    // navigate back into a completed handover flow.
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onLotClick = { sessionId ->
                    navController.navigate(Destinations.handoverRoute(sessionId))
                }
            )
        }
    }
}
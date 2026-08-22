package com.example.scancard.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.scancard.ui.deckdetail.DeckDetailScreen
import com.example.scancard.ui.export.ExportScreen
import com.example.scancard.ui.extraction.ExtractionPreviewScreen
import com.example.scancard.ui.home.HomeScreen
import com.example.scancard.ui.scan.ScanScreen
import com.example.scancard.ui.study.StudyScreen

@Composable
fun ScanCardNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onScanClick = { navController.navigate(Screen.Scan.createRoute(0L)) },
                onDeckClick = { deckId -> navController.navigate(Screen.DeckDetail.createRoute(deckId)) }
            )
        }
        composable(
            route = Screen.Scan.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            ScanScreen(
                onBack = { navController.popBackStack() },
                onComplete = { newDeckId -> navController.navigate(Screen.ExtractionPreview.createRoute(newDeckId)) },
                deckId = deckId
            )
        }
        composable(
            route = Screen.ExtractionPreview.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            ExtractionPreviewScreen(
                onBack = { navController.popBackStack() },
                onFinish = { id -> 
                    navController.navigate(Screen.DeckDetail.createRoute(id)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                deckId = deckId
            )
        }
        composable(
            route = Screen.DeckDetail.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) {
            DeckDetailScreen(
                onBack = { navController.popBackStack() },
                onStudyClick = { deckId -> navController.navigate(Screen.Study.createRoute(deckId)) },
                onExportClick = { deckId -> navController.navigate(Screen.Export.createRoute(deckId)) },
                onAddByScanClick = { deckId -> navController.navigate(Screen.Scan.createRoute(deckId)) }
            )
        }
        composable(
            route = Screen.Study.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) {
            StudyScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) {
            ExportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

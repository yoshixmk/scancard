package com.plath.scancard.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.plath.scancard.ui.deckdetail.DeckDetailScreen
import com.plath.scancard.ui.export.ExportScreen
import com.plath.scancard.ui.extraction.ExtractionPreviewScreen
import com.plath.scancard.ui.home.HomeScreen
import com.plath.scancard.ui.scan.ScanScreen
import com.plath.scancard.ui.study.StudyScreen

import androidx.navigation.navDeepLink

object NavHolder {
    var navController: NavHostController? = null
}
// TODO(IMP-02): Migrate to NavDisplay + entryProvider (see docs/navigation3-migration.md)
// Migration procedure (Run with JVM17 + AGP 9.3.1, comments only to avoid full gradle execution):
//   1. Uncomment TODO(IMP-02) in app/build.gradle.kts and Sync Navigation3 dependencies (navigation3-runtime/ui + kotlin-serialization plugin)
//   2. Uncomment ": NavKey" in ui/navigation/NavKeys.kt (import androidx.navigation3.runtime.NavKey)
//   3. Replace NavHost/composable/navArgument/navDeepLink in this file with the following After example:
//      ```
//      // Before:
//      NavHost(navController, startDestination = Screen.Home.route) {
//          composable(Screen.Home.route) { HomeScreen(onScanClick = { navController.navigate(Screen.Scan.createRoute(0L)) }) }
//          composable("scan/{deckId}", arguments=listOf(navArgument("deckId"){type=LongType})) { entry -> val id = entry.arguments?.getLong("deckId") }
//      }
//      // After:
//      val backStack = rememberNavBackStack(Home) // or rememberSaveableBackStack(Home)
//      NavDisplay(
//          backStack = backStack,
//          onBack = { backStack.removeLastOrNull() },
//          entryProvider = entryProvider {
//              entry<Home> { HomeScreen(onScanClick = { backStack.add(Scan(deckId=0L)) }) }
//              entry<Scan> { key -> ScanScreen(deckId = key.deckId, onBack = { backStack.removeLastOrNull() }) }
//              entry<DeckDetail> { key -> DeckDetailScreen(deckId = key.deckId) } // Deep links migrated to UriDeepLinkMatcher("scancard://deck/{deckId}")
//          }
//      )
//      ```
//   4. Callback replacement proposals (no side effects):
//      - navController.navigate(Screen.Scan.createRoute(id))           -> backStack.add(Scan(deckId=id))
//      - navController.navigate(Screen.DeckDetail.createRoute(id))     -> backStack.add(DeckDetail(deckId=id))
//      - navController.navigate(Screen.ExtractionPreview.createRoute(id)) -> backStack.add(ExtractionPreview(deckId=id))
//      - navController.navigate(Screen.Study.createRoute(id))          -> backStack.add(Study(deckId=id))
//      - navController.navigate(Screen.Export.createRoute(id))         -> backStack.add(Export(deckId=id))
//      - navController.popBackStack()                                  -> backStack.removeLastOrNull()
//      - navController.navigate(...){ popUpTo(Home) }                  -> while(backStack.lastOrNull()!=Home) backStack.removeLastOrNull(); backStack.add(...)
//      - backStackEntry.arguments?.getLong("deckId")                   -> key.deckId  or  savedStateHandle.toRoute<DeckDetail>().deckId
//      - navDeepLink { uriPattern="scancard://deck/{deckId}" }         -> UriDeepLinkMatcher(DeepLinkUri("scancard://deck/{deckId}"), serializer<DeckDetail>()) + DeepLinkRequest(intent)
//   5. See docs/navigation3-migration.md and .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step3-6 for details
//   6. Verification: Ensure ./gradlew :app:testDebugUnitTest + :app:assembleDebug SUCCESS on JVM17, uncomment NavigationTest template and verify green.
// TODO(IMP-05): Adaptive ListDetail Scene — Enable after Navigation3 migration (adaptive/SKILL.md Step3)
// Activation procedure (After IMP-02 Nav3 migration):
// 1. Uncomment adaptive-navigation3 in app/build.gradle.kts
// 2. import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
//    import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
//    import androidx.navigation3.runtime.NavDisplay
//    import androidx.navigation3.runtime.entryProvider
//    import androidx.navigation3.runtime.rememberSaveableBackStack
// 3. Replace this NavHost with NavDisplay:
// ```
// val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
// NavDisplay(
//     backStack = backStack, // rememberSaveableBackStack(Home)
//     sceneStrategy = listDetailStrategy,
//     entryProvider = entryProvider {
//         entry<Home>(metadata = ListDetailSceneStrategy.listPane(
//             detailPlaceholder = { Text("Please select a deck", modifier=Modifier.fillMaxSize().wrapContentSize()) }
//         )) { HomeScreen(...) }
//         entry<DeckDetail>(metadata = ListDetailSceneStrategy.detailPane()) { DeckDetailScreen(...) }
//         // Study/Scan/Export are regular entries, not listPane or detailPane
//     }
// )
// ```
// Currently remains as NavHost of navigation-compose:2.9.8 — commented out to avoid breaking the build.

@Composable
fun ScanCardNavHost(navController: NavHostController) {
    androidx.compose.runtime.LaunchedEffect(navController) { NavHolder.navController = navController }
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
                onComplete = { newDeckId, fastMode ->
                    // Fast Mode (Req18.3): Skip extraction preview if model is ready
                    // and navigate directly to DeckDetail (backstack pops to Home).
                    if (fastMode) {
                        navController.navigate(Screen.DeckDetail.createRoute(newDeckId)) {
                            popUpTo(Screen.Home.route)
                        }
                    } else {
                        navController.navigate(Screen.ExtractionPreview.createRoute(newDeckId))
                    }
                },
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
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "scancard://deck/{deckId}" }
            )
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

// TODO(IMP-05 5-7): Apply @FormFactorPreviews to each Screen Preview — ScanCardNavHost itself is not a Preview target
// as it's a NavHost, but verify Home/DeckDetail are displayed side-by-side in ListDetailScene using
// @FormFactorPreviews (700dp/900dp/1200dp) with screenshots.
// Procedure: See .kiro/skills/adaptive/SKILL.md Step3.3.

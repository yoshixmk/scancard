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
// TODO(IMP-02): NavDisplay + entryProvider に移行 (docs/navigation3-migration.md参照)
// 移行手順 (JVM17 + AGP 9.3.1 で実行、gradleフル実行禁止のためコメントのみ):
//   1. app/build.gradle.kts の TODO(IMP-02) をアンコメントし Navigation3 依存を Sync (navigation3-runtime/ui + kotlin-serialization plugin)
//   2. ui/navigation/NavKeys.kt の `: NavKey` をアンコメント (import androidx.navigation3.runtime.NavKey)
//   3. 本ファイルの NavHost/composable/navArgument/navDeepLink を下記 After 例に置換:
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
//              entry<DeckDetail> { key -> DeckDetailScreen(deckId = key.deckId) } // deepLinkは UriDeepLinkMatcher("scancard://deck/{deckId}") に移行
//          }
//      )
//      ```
//   4. コールバック置換案 (副作用なし):
//      - navController.navigate(Screen.Scan.createRoute(id))           -> backStack.add(Scan(deckId=id))
//      - navController.navigate(Screen.DeckDetail.createRoute(id))     -> backStack.add(DeckDetail(deckId=id))
//      - navController.navigate(Screen.ExtractionPreview.createRoute(id)) -> backStack.add(ExtractionPreview(deckId=id))
//      - navController.navigate(Screen.Study.createRoute(id))          -> backStack.add(Study(deckId=id))
//      - navController.navigate(Screen.Export.createRoute(id))         -> backStack.add(Export(deckId=id))
//      - navController.popBackStack()                                  -> backStack.removeLastOrNull()
//      - navController.navigate(...){ popUpTo(Home) }                  -> while(backStack.lastOrNull()!=Home) backStack.removeLastOrNull(); backStack.add(...)
//      - backStackEntry.arguments?.getLong("deckId")                   -> key.deckId  or  savedStateHandle.toRoute<DeckDetail>().deckId
//      - navDeepLink { uriPattern="scancard://deck/{deckId}" }         -> UriDeepLinkMatcher(DeepLinkUri("scancard://deck/{deckId}"), serializer<DeckDetail>()) + DeepLinkRequest(intent)
//   5. 詳細は docs/navigation3-migration.md および .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step3-6 参照
//   6. 検証: JVM17で ./gradlew :app:testDebugUnitTest + :app:assembleDebug が SUCCESS、NavigationTest 雛形をアンコメントし緑化
// TODO(IMP-05): Adaptive ListDetail Scene — Navigation3移行後に有効化 (adaptive/SKILL.md Step3)
// 有効化手順 (IMP-02 Nav3移行後):
// 1. app/build.gradle.kts の adaptive-navigation3 コメントを外す
// 2. import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
//    import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
//    import androidx.navigation3.runtime.NavDisplay
//    import androidx.navigation3.runtime.entryProvider
//    import androidx.navigation3.runtime.rememberSaveableBackStack
// 3. 本 NavHost を NavDisplay に置換:
// ```
// val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
// NavDisplay(
//     backStack = backStack, // rememberSaveableBackStack(Home)
//     sceneStrategy = listDetailStrategy,
//     entryProvider = entryProvider {
//         entry<Home>(metadata = ListDetailSceneStrategy.listPane(
//             detailPlaceholder = { Text("デッキを選択してください", modifier=Modifier.fillMaxSize().wrapContentSize()) }
//         )) { HomeScreen(...) }
//         entry<DeckDetail>(metadata = ListDetailSceneStrategy.detailPane()) { DeckDetailScreen(...) }
//         // Study/Scan/Export は detailPane でも supportingPane でもなく通常 entry
//     }
// )
// ```
// 現状は navigation-compose:2.9.8 の NavHost のまま — ビルドを壊さないためコメント留め。

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

// TODO(IMP-05 5-7): 各Screen Preview に @FormFactorPreviews 適用 — ScanCardNavHost自体は NavHost のため
// Preview対象外だが、Home/DeckDetail は ListDetailScene で並列表示されることを
// @FormFactorPreviews (700dp/900dp/1200dp) でスクショ検証すること。
// 手順: `.kiro/skills/adaptive/SKILL.md` Step3.3 参照。

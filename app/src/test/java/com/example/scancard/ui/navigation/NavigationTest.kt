package com.example.scancard.ui.navigation

/**
 * IMP-02 Navigation3 テスト雛形 (JVM8制約でコメント中心、Navigation3依存なしでコンパイル可能)
 *
 * 背景: Navigation2 (NavHost/composable/navArgument) → Navigation3 (NavDisplay/entryProvider/NavKey) 移行の
 *       テストを Robolectric + ComposeTestRule で検証する雛形。
 *       本ファイルは navigation3-runtime が未導入でもコンパイルが通るよう、Navigation3 API の使用箇所は
 *       コメントアウトしている。有効化時はコメントを外し、JVM17 + AGP 9.3.1 で実行すること。
 *
 * 前提:
 *   - app/build.gradle.kts の TODO(IMP-02) を有効化し Navigation3 依存を Sync 済みであること
 *   - ui/navigation/NavKeys.kt の `: NavKey` を有効化済みであること
 *   - docs/navigation3-migration.md の移行手順を完了済みであること
 *
 * 実行:
 *   ./gradlew :app:testDebugUnitTest --tests "com.example.scancard.ui.navigation.NavigationTest"
 *
 * 参考:
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicdsl.md
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicsaveable.md
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/deeplinks-staticuri.md
 *   - testing-setup/SKILL.md Step8 (Compose UI behavior test)
 */

// import androidx.compose.ui.test.assertIsDisplayed
// import androidx.compose.ui.test.junit4.createComposeRule
// import androidx.compose.ui.test.onNodeWithText
// import androidx.compose.ui.test.performClick
// import androidx.navigation3.runtime.NavKey
// import androidx.navigation3.runtime.entryProvider
// import androidx.navigation3.runtime.rememberNavBackStack
// import androidx.navigation3.ui.NavDisplay
// import androidx.test.ext.junit.runners.AndroidJUnit4
// import kotlinx.serialization.Serializable
// import org.junit.Rule
// import org.junit.Test
// import org.junit.runner.RunWith
// import org.robolectric.annotation.Config
// import com.google.common.truth.Truth.assertThat

// TODO(IMP-02): Navigation3 有効化時に下記アノテーションを有効化
// @RunWith(AndroidJUnit4::class)
// @Config(sdk = [34]) // Robolectric SDK 34で実行
class NavigationTest {

    // TODO(IMP-02): 有効化時は ComposeTestRule を使用
    // @get:Rule val composeTestRule = createComposeRule()

    // --- 雛形1: Home → DeckDetail 遷移テスト ---
    // @Test
    // fun home_navigatesToDeckDetail_onDeckClick() {
    //     composeTestRule.setContent {
    //         val backStack = rememberNavBackStack(Home)
    //         NavDisplay(
    //             backStack = backStack,
    //             onBack = { backStack.removeLastOrNull() },
    //             entryProvider = entryProvider {
    //                 entry<Home> {
    //                     // HomeScreen(onDeckClick = { deckId -> backStack.add(DeckDetail(deckId)) })
    //                     // テスト用簡易UI:
    //                     androidx.compose.material3.Button(onClick = { backStack.add(DeckDetail(deckId = 42L)) }) {
    //                         androidx.compose.material3.Text("Go to DeckDetail")
    //                     }
    //                 }
    //                 entry<DeckDetail> { key ->
    //                     androidx.compose.material3.Text("DeckDetail id=${key.deckId}")
    //                 }
    //             }
    //         )
    //     }
    //     composeTestRule.onNodeWithText("Go to DeckDetail").performClick()
    //     composeTestRule.onNodeWithText("DeckDetail id=42").assertIsDisplayed()
    //     // backStack.last() == DeckDetail(42) を assertThat で検証
    // }

    // --- 雛形2: 戻る挙動テスト ---
    // @Test
    // fun backStack_removeLastOrNull_popsToHome() {
    //     composeTestRule.setContent {
    //         val backStack = rememberNavBackStack(Home)
    //         NavDisplay(
    //             backStack = backStack,
    //             onBack = { backStack.removeLastOrNull() },
    //             entryProvider = entryProvider {
    //                 entry<Home> {
    //                     androidx.compose.material3.Button(onClick = { backStack.add(Scan(deckId = 1L)) }) {
    //                         androidx.compose.material3.Text("Home")
    //                     }
    //                 }
    //                 entry<Scan> { key ->
    //                     androidx.compose.material3.Text("Scan id=${key.deckId}")
    //                 }
    //             }
    //         )
    //     }
    //     composeTestRule.onNodeWithText("Home").performClick()
    //     composeTestRule.onNodeWithText("Scan id=1").assertIsDisplayed()
    //     // システムバックをシミュレート: backStack.removeLastOrNull() を直接呼ぶか、ComposeのBackHandler経由
    //     // composeTestRule.activity.onBackPressed() 等で検証
    //     // 期待: backStack.size == 1 && backStack.last() == Home
    // }

    // --- 雛形3: 複数バックスタック (Home ↔ Detail) ---
    // 将来 BottomNav / Adaptive ListDetail 導入時に有効化
    // @Test
    // fun multipleBackStacks_retainStateOnTabSwitch() {
    //     // migration-guide.md Step3 の NavigationState / Navigator を使用
    //     // val navigationState = rememberNavigationState(startRoute = Home, topLevelRoutes = setOf(Home, Settings))
    //     // val navigator = remember { Navigator(navigationState) }
    //     // NavDisplay(entries = navigationState.toEntries(entryProvider), onBack = { navigator.goBack() })
    //     // タブ切替で backStacks[topLevelRoute] が保持されることを assertThat で検証
    // }

    // --- 雛形4: DeepLink scancard://deck/{deckId} テスト ---
    // @Test
    // fun deepLink_scancardDeck_matchesDeckDetail() {
    //     // import androidx.navigation3.runtime.deeplink.DeepLinkRequest
    //     // import androidx.navigation3.runtime.deeplink.DeepLinkUri
    //     // import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
    //     // import kotlinx.serialization.serializer
    //     // val matcher = UriDeepLinkMatcher(
    //     //     uriPattern = DeepLinkUri("scancard://deck/{deckId}"),
    //     //     serializer = serializer<DeckDetail>()
    //     // )
    //     // val intent = Intent().apply { data = "scancard://deck/99".toUri() }
    //     // val request = DeepLinkRequest(intent)
    //     // val result = matcher.match(request)
    //     // assertThat(result?.key).isEqualTo(DeckDetail(deckId = 99L))
    //     // // Synthetic backStack: rememberNavBackStack(Home, DeckDetail(99)) で Home→Detail の階層を再現
    // }

    // --- 雛形5: ExtractionPreview → DeckDetail の popUpTo(Home) 相当テスト ---
    // @Test
    // fun extractionPreview_onFinish_clearsBackStackToHome() {
    //     // composeTestRule.setContent {
    //     //     val backStack = rememberNavBackStack(Home, Scan(0L), ExtractionPreview(5L))
    //     //     NavDisplay(
    //     //         backStack = backStack,
    //     //         onBack = { backStack.removeLastOrNull() },
    //     //         entryProvider = entryProvider {
    //     //             entry<Home> { Text("Home") }
    //     //             entry<Scan> { Text("Scan") }
    //     //             entry<ExtractionPreview> { key ->
    //     //                 Button(onClick = {
    //     //                     while (backStack.lastOrNull() != Home) backStack.removeLastOrNull()
    //     //                     backStack.add(DeckDetail(deckId = key.deckId))
    //     //                 }) { Text("Finish") }
    //     //             }
    //     //             entry<DeckDetail> { key -> Text("DeckDetail ${key.deckId}") }
    //     //         }
    //     //     )
    //     // }
    //     // composeTestRule.onNodeWithText("Finish").performClick()
    //     // composeTestRule.onNodeWithText("DeckDetail 5").assertIsDisplayed()
    //     // // backStack == [Home, DeckDetail(5)] であることを検証
    // }

    // --- 雛形6: DeviceConfigurationOverride で windowSize/fontScale シミュレーション ---
    // @Test
    // fun adaptive_listDetail_scene_isDisplayedOnTablet() {
    //     // composeTestRule.setContent {
    //     //     DeviceConfigurationOverride(
    //     //         DeviceConfigurationOverride.WindowSize(widthDp = 900, heightDp = 600),
    //     //         DeviceConfigurationOverride.FontScale(1.5f)
    //     //     ) {
    //     //         val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    //     //         NavDisplay(
    //     //             backStack = rememberNavBackStack(Home),
    //     //             sceneStrategy = listDetailStrategy,
    //     //             entryProvider = entryProvider {
    //     //                 entry<Home>(metadata = ListDetailSceneStrategy.listPane()) { Text("List") }
    //     //                 entry<DeckDetail>(metadata = ListDetailSceneStrategy.detailPane()) { Text("Detail") }
    //     //             }
    //     //         )
    //     //     }
    //     // }
    // }
}

// 注意: 本ファイルは雛形のためテストケースはコメントアウト済み。Navigation3 有効化時にアンコメントし、
//       `Truth.assertThat` / `composeTestRule.onNodeWithText` 等で検証を実装すること。
//       現状はコンパイルエラーを避けるため import もコメント化している。

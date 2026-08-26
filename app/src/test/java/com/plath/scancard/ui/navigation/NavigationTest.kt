package com.plath.scancard.ui.navigation

/**
 * IMP-02 Navigation3 Test Template (Comment-based to remain compilable without Navigation3
 * dependencies under JVM8 constraints)
 *
 * Background: Template for verifying Navigation2 (NavHost/composable/navArgument) ->
 *       Navigation3 (NavDisplay/entryProvider/NavKey) migration with Robolectric + ComposeTestRule.
 *       This file has Navigation3 API calls commented out so it compiles even without
 *       navigation3-runtime. Uncomment and run with JVM17 + AGP 9.3.1 upon activation.
 *
 * Prerequisites:
 *   - TODO(IMP-02) in `app/build.gradle.kts` enabled and Navigation3 dependencies synced.
 *   - ": NavKey" in `ui/navigation/NavKeys.kt` enabled.
 *   - Migration steps in `docs/navigation3-migration.md` completed.
 *
 * Execution:
 *   `./gradlew :app:testDebugUnitTest --tests "com.plath.scancard.ui.navigation.NavigationTest"`
 *
 * References:
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

// TODO(IMP-02): Enable the following annotations when activating Navigation3
// @RunWith(AndroidJUnit4::class)
// @Config(sdk = [34]) // Run on Robolectric SDK 34
class NavigationTest {

    // TODO(IMP-02): Use ComposeTestRule upon activation
    // @get:Rule val composeTestRule = createComposeRule()

    // --- Template 1: Home -> DeckDetail transition test ---
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
    //                     // Simplified UI for testing:
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
    //     // Verify backStack.last() == DeckDetail(42) with assertThat
    // }

    // --- Template 2: Back behavior test ---
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
    //     // Simulate system back: call backStack.removeLastOrNull() directly or via Compose's BackHandler.
    //     // Verify with composeTestRule.activity.onBackPressed(), etc.
    //     // Expectation: backStack.size == 1 && backStack.last() == Home
    // }

    // --- Template 3: Multiple BackStacks (Home <-> Detail) ---
    // Enable upon future introduction of BottomNav / Adaptive ListDetail
    // @Test
    // fun multipleBackStacks_retainStateOnTabSwitch() {
    //     // Use NavigationState / Navigator from migration-guide.md Step 3
    //     // val navigationState = rememberNavigationState(startRoute = Home, topLevelRoutes = setOf(Home, Settings))
    //     // val navigator = remember { Navigator(navigationState) }
    //     // NavDisplay(entries = navigationState.toEntries(entryProvider), onBack = { navigator.goBack() })
    //     // Verify that backStacks[topLevelRoute] is retained during tab switching using assertThat
    // }

    // --- Template 4: DeepLink scancard://deck/{deckId} test ---
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
    //     // // Synthetic backStack: Reproduce Home->Detail hierarchy with rememberNavBackStack(Home, DeckDetail(99))
    // }

    // --- Template 5: ExtractionPreview -> DeckDetail equivalent of popUpTo(Home) test ---
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
    //     // // Verify backStack == [Home, DeckDetail(5)]
    // }

    // --- Template 6: windowSize/fontScale simulation via DeviceConfigurationOverride ---
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

/**
 * Note: This file is a template, so test cases are commented out. Uncomment and implement
 * verification using Truth.assertThat / composeTestRule.onNodeWithText, etc., when
 * activating Navigation3. Imports are also commented out to avoid compilation errors.
 */

package com.plath.scancard.ui.home

// =============================================================================
// IMP-06 6-3 Screenshot Test Template (Compose Preview Screenshot Testing)
// =============================================================================
// Constraints: Full gradle execution and screenshotTest task are not available on JVM8.
//       This file is created as a "comment-based template". Even if it doesn't compile,
//       keep it as comments and gradually enable it after JVM17 + plugin activation.
// All implementations are commented out to avoid breaking the build. Enable by
// uncommenting TODO(IMP-06) in app/build.gradle.kts as needed.
// =============================================================================

// TODO(IMP-06) 6-1 Uncomment after enabling dependencies:
// plugins {
//     alias(libs.plugins.compose.screenshotTest) // or id("com.android.compose.screenshot")
// }
// Once enabled, sourceSet: `app/src/screenshotTest/java/` will be automatically recognized,
// and `./gradlew :app:validateScreenshotTest` / `updateScreenshotTest` will become available.

// -----------------------------------------------------------------------------
// Imports to be uncommented upon activation
// -----------------------------------------------------------------------------
// import androidx.compose.runtime.Composable
// import androidx.compose.ui.tooling.preview.Devices
// import androidx.compose.ui.tooling.preview.Preview
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.ui.test.DeviceConfigurationOverride
// import androidx.compose.ui.test.WindowSize
// import com.android.compose.screenshot.PreviewTest // For AGP 8.5+ Preview Screenshot Testing
// import com.plath.scancard.ui.theme.ScanCardTheme
// import com.plath.scancard.data.local.entities.Deck
// import java.util.Date

// -----------------------------------------------------------------------------
// FormFactorPreviews annotation template (adheres to adaptive/SKILL.md Step 1)
// Planned to be defined in ui/preview/FormFactorPreviews.kt in IMP-05.
// Re-listed here for reference during activation.
// -----------------------------------------------------------------------------
// @Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
// @Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
// @Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
// @Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
// annotation class FormFactorPreviews

// =============================================================================
// HomeScreen screenshot template — 9 sizes (400/610/900dp x 400/500/1000dp)
// + dark/light + fontScale1.5
// =============================================================================

// -----------------------------------------------------------------------------
// Option A: Automatic collection with @PreviewTest (Recommended for Compose Preview Screenshot Testing)
// Each @PreviewTest + @FormFactorPreviews generates all sizes via validateScreenshotTest
// -----------------------------------------------------------------------------

// // Empty state — Phone (400x500) as baseline, other sizes auto-generated via FormFactorPreviews
// @PreviewTest
// @FormFactorPreviews
// @Composable
// fun HomeScreenScreenshot_Empty() {
//     ScanCardTheme {
//         // Swap with Fake ViewModel using Hilt, or use a Wrapper that takes decks as an argument.
//         // Example: HomeScreen(decks = emptyList(), onScanClick = {}, onDeckClick = {})
//         // Since the current HomeScreen uses hiltViewModel() directly, it is recommended
//         // to extract HomeScreenContent(decks=...) for Screenshots.
//         HomeScreenPreviewWrapper(decks = emptyList())
//     }
// }
//
// // With decks — displaying multiple decks
// @PreviewTest
// @FormFactorPreviews
// @Composable
// fun HomeScreenScreenshot_WithDecks() {
//     ScanCardTheme {
//         HomeScreenPreviewWrapper(
//             decks = listOf(
//                 Deck(id = 1, title = "English Words", createdAt = Date()),
//                 Deck(id = 2, title = "Japanese Vocab", createdAt = Date()),
//                 Deck(id = 3, title = "Long deck title that may wrap to second line", createdAt = Date()),
//             )
//         )
//     }
// }
//
// // Dark theme variation (mobile 400x500 only is fine, adheres to Skill Step 8)
// @Preview(name = "Dark", device = Devices.PHONE, showBackground = true)
// @PreviewTest
// @Composable
// fun HomeScreenScreenshot_Dark() {
//     ScanCardTheme(darkTheme = true) {
//         HomeScreenPreviewWrapper(
//             decks = listOf(Deck(id = 1, title = "Dark Deck", createdAt = Date()))
//         )
//     }
// }
//
// // FontScale 1.5 variation (mobile 400x500 only)
// // Overwrite fontScale via DeviceConfigurationOverride — see docs/testing.md addition
// @PreviewTest
// @Composable
// fun HomeScreenScreenshot_FontScale15() {
//     // Upon activation: Reproduce fontScale=1.5f via CompositionLocalProvider + DeviceConfigurationOverride
//     // DeviceConfigurationOverride(
//     //     DeviceConfigurationOverride.FontScale(1.5f)
//     // ) {
//     //     ScanCardTheme { HomeScreenPreviewWrapper(decks = listOf(Deck(id=1, title="Large Font Deck", createdAt=Date()))) }
//     // }
// }

// -----------------------------------------------------------------------------
// Option B: Explicitly hand-written Previews for 9 sizes (substituted via LayoutLib device specification)
// Useful for visual verification of variable column counts in Adaptive Grid (GridCells.Adaptive 320dp)
// -----------------------------------------------------------------------------
// @Preview(name = "400×400", widthDp = 400, heightDp = 400, showBackground = true)
// @Preview(name = "400×500", widthDp = 400, heightDp = 500, showBackground = true)
// @Preview(name = "400×1000", widthDp = 400, heightDp = 1000, showBackground = true)
// @Preview(name = "610×400", widthDp = 610, heightDp = 400, showBackground = true)
// @Preview(name = "610×500", widthDp = 610, heightDp = 500, showBackground = true)
// @Preview(name = "610×1000", widthDp = 610, heightDp = 1000, showBackground = true)
// @Preview(name = "900×400", widthDp = 900, heightDp = 400, showBackground = true)
// @Preview(name = "900×500", widthDp = 900, heightDp = 500, showBackground = true)
// @Preview(name = "900×1000", widthDp = 900, heightDp = 1000, showBackground = true)
// @PreviewTest
// @Composable
// fun HomeScreenScreenshot_9Sizes() {
//     ScanCardTheme { HomeScreenPreviewWrapper(decks = sampleDecks()) }
// }

// -----------------------------------------------------------------------------
// Optional: Switching template for Paparazzi / Roborazzi (Future options, commented out due to JVM8 constraints)
// -----------------------------------------------------------------------------
// // Paparazzi:
// // @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, theme = "android:Theme.Material3.DayNight")
// // @Test fun homeScreen_paparazzi() { paparazzi.snapshot { ScanCardTheme { HomeScreenPreviewWrapper(sampleDecks()) } } }
//
// // Roborazzi (Robolectric):
// // @Test fun homeScreen_roborazzi() {
// //     composeRule.setContent { ScanCardTheme { HomeScreenPreviewWrapper(sampleDecks()) } }
// //     composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/HomeScreen.png")
// // }
//
// // Dropshots (device):
// // @get:Rule val dropshots = Dropshots()
// // @Test fun homeScreen_dropshots() {
// //     composeRule.setContent { ScanCardTheme { HomeScreenPreviewWrapper(sampleDecks()) } }
// //     dropshots.assertSnapshot(composeRule.onRoot())
// // }

// -----------------------------------------------------------------------------
// Preview Wrapper template — Content separation to decouple ViewModel dependency
// Since current HomeScreen uses hiltViewModel(), a stateless Wrapper is preferred for Screenshots
// Future: Split HomeScreen.kt into HomeScreen(viewModel) and HomeScreenContent(decks, ...)
// -----------------------------------------------------------------------------
// @Composable
// private fun HomeScreenPreviewWrapper(decks: List<Deck>) {
//     // HomeScreenContent(decks = decks, onScanClick = {}, onDeckClick = {}, onDelete = {})
//     // DeckItem Preview can also be separated to add component-level screenshots
// }
//
// private fun sampleDecks() = listOf(
//     Deck(id = 1, title = "Sample Deck 1", createdAt = Date()),
//     Deck(id = 2, title = "Sample Deck 2", createdAt = Date()),
// )

// =============================================================================
// Activation Procedure Summary
// =============================================================================
// 1. Uncomment TODO(IMP-06) in `app/build.gradle.kts` and Sync (with JVM17)
// 2. Uncomment the import / @PreviewTest blocks in this file
// 3. Separate Content in `HomeScreen.kt` (if needed) so the Wrapper can reference it
// 4. Generate reference images with `./gradlew :app:updateScreenshotTest` -> visual approval
// 5. Difference verification with `./gradlew :app:validateScreenshotTest` (CI)
// 6. Future: Add DeckDetailScreen / StudyScreen / ExtractionPreviewScreen under screenshotTest in the same way
//    - `app/src/screenshotTest/java/com/plath/scancard/ui/deckdetail/DeckDetailScreenScreenshotTest.kt`
//    - `app/src/screenshotTest/java/com/plath/scancard/ui/study/StudyScreenScreenshotTest.kt`
//    - Add @PreviewTest for each state (empty/loading/success/error) in each file
// =============================================================================

// NOTE: This file is a template and currently not a compilation target (all commented out).
// It does not affect the build until screenshotTest sourceSet is enabled and comments
// above are removed.

package com.plath.scancard.ui.home

// =============================================================================
// IMP-06 6-3 Screenshot Test 雛形 (Compose Preview Screenshot Testing)
// =============================================================================
// 制約: gradleフル実行禁止 / JVM8では screenshotTest タスク実行不可 のため
//       本ファイルは「雛形コメント中心」で作成。実際にコンパイルが通らなくても
//       コメントとして残し、JVM17 + plugin有効化後に段階的に有効化する。
// ビルドを壊さないため、実装は全てコメントアウトで残す。必要に応じて
// app/build.gradle.kts の TODO(IMP-06) コメントを外してから有効化すること。
// =============================================================================

// TODO(IMP-06) 6-1 依存有効化後にアンコメント:
// plugins {
//     alias(libs.plugins.compose.screenshotTest) // or id("com.android.compose.screenshot")
// }
// 有効化後の sourceSet: `app/src/screenshotTest/java/` が自動認識され、
// `./gradlew :app:validateScreenshotTest` / `updateScreenshotTest` が利用可能になる。

// -----------------------------------------------------------------------------
// 有効化時にアンコメントする import 群
// -----------------------------------------------------------------------------
// import androidx.compose.runtime.Composable
// import androidx.compose.ui.tooling.preview.Devices
// import androidx.compose.ui.tooling.preview.Preview
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.ui.test.DeviceConfigurationOverride
// import androidx.compose.ui.test.WindowSize
// import com.android.compose.screenshot.PreviewTest // AGP 8.5+ の Preview Screenshot Testing 用
// import com.plath.scancard.ui.theme.ScanCardTheme
// import com.plath.scancard.data.local.entities.Deck
// import java.util.Date

// -----------------------------------------------------------------------------
// FormFactorPreviews アノテーション雛形 (adaptive/SKILL.md Step1準拠)
// IMP-05で `ui/preview/FormFactorPreviews.kt` に定義予定。ここでは再掲（有効化時にそちらを参照）。
// -----------------------------------------------------------------------------
// @Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
// @Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
// @Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
// @Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
// annotation class FormFactorPreviews

// =============================================================================
// HomeScreen スクショ雛形 — 9サイズ (400/610/900dp × 400/500/1000dp)
// + dark/light + fontScale1.5
// =============================================================================

// -----------------------------------------------------------------------------
// 案A: @PreviewTest で自動収集（Compose Preview Screenshot Testing 推奨）
// 各 @PreviewTest + @FormFactorPreviews が validateScreenshotTest で全サイズ生成
// -----------------------------------------------------------------------------

// // Empty state — Phone (400×500) が基準、他サイズは FormFactorPreviews で自動生成
// @PreviewTest
// @FormFactorPreviews
// @Composable
// fun HomeScreenScreenshot_Empty() {
//     ScanCardTheme {
//         // Fake ViewModel を Hilt で差し替え、または HomeScreen の decks を引数化したWrapperを使用
//         // 例: HomeScreen(decks = emptyList(), onScanClick = {}, onDeckClick = {})
//         // 現行 HomeScreen は hiltViewModel() を直接使うため、Screenshot用に `HomeScreenContent(decks=...)` を切り出すのが推奨
//         HomeScreenPreviewWrapper(decks = emptyList())
//     }
// }
//
// // With decks — 複数デッキ表示
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
// // Dark theme バリエーション (mobile 400×500 のみでも可、Skill Step8準拠)
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
// // FontScale 1.5 バリエーション (mobile 400×500 のみ)
// // DeviceConfigurationOverride で fontScale を上書き — docs/testing.md の追記を参照
// @PreviewTest
// @Composable
// fun HomeScreenScreenshot_FontScale15() {
//     // 有効化時: CompositionLocalProvider + DeviceConfigurationOverride で fontScale=1.5f を再現
//     // DeviceConfigurationOverride(
//     //     DeviceConfigurationOverride.FontScale(1.5f)
//     // ) {
//     //     ScanCardTheme { HomeScreenPreviewWrapper(decks = listOf(Deck(id=1, title="Large Font Deck", createdAt=Date()))) }
//     // }
// }

// -----------------------------------------------------------------------------
// 案B: 9サイズを明示的に手書きPreview（LayoutLibのdevice指定で代替）
// Adaptive Grid (GridCells.Adaptive 320dp) の列数可変を目視確認するために有効
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
// Optional: Paparazzi / Roborazzi への切替雛形（将来の選択肢、JVM8制約でコメント留め）
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
// Preview Wrapper 雛形 — ViewModel依存を切り離すための Content 分離案
// 現行 HomeScreen は hiltViewModel() を使うため、Screenshotでは stateless なWrapperが望ましい
// 将来: HomeScreen.kt を `HomeScreen(viewModel)` と `HomeScreenContent(decks, ...)` に分割
// -----------------------------------------------------------------------------
// @Composable
// private fun HomeScreenPreviewWrapper(decks: List<Deck>) {
//     // HomeScreenContent(decks = decks, onScanClick = {}, onDeckClick = {}, onDelete = {})
//     // DeckItem の Preview も同様に分離して Component-level screenshot を追加可能
// }
//
// private fun sampleDecks() = listOf(
//     Deck(id = 1, title = "Sample Deck 1", createdAt = Date()),
//     Deck(id = 2, title = "Sample Deck 2", createdAt = Date()),
// )

// =============================================================================
// 有効化手順まとめ
// =============================================================================
// 1. app/build.gradle.kts の TODO(IMP-06) コメントを外し Sync（JVM17で）
// 2. 本ファイルの import / @PreviewTest ブロックのコメントを外す
// 3. HomeScreen.kt を Content分離（必要なら）し、Wrapperが参照できるようにする
// 4. ./gradlew :app:updateScreenshotTest で参照画像を生成 → 目視承認
// 5. ./gradlew :app:validateScreenshotTest で差分検証（CI）
// 6. 将来: DeckDetailScreen / StudyScreen / ExtractionPreviewScreen も同様に screenshotTest 配下に追加
//    - `app/src/screenshotTest/java/com/example/scancard/ui/deckdetail/DeckDetailScreenScreenshotTest.kt`
//    - `app/src/screenshotTest/java/com/example/scancard/ui/study/StudyScreenScreenshotTest.kt`
//    - 各ファイルで empty/loading/success/error の状態別に @PreviewTest を追加
// =============================================================================

// NOTE: 本ファイルは雛形のため、現状はコンパイル対象外（全てコメント）。screenshotTest sourceSet
// が有効化され上記コメントを外すまではビルドに影響しない。

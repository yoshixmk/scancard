package com.plath.scancard.e2e

// =============================================================================
// IMP-06 6-4 UIAutomator E2E 雛形 (5% E2Eカバレッジ目標)
// =============================================================================
// 制約: gradleフル実行禁止 / JVM8では connectedDebugAndroidTest 実行不可 のため
//       本ファイルは「雛形コメント中心」で作成。実際にコンパイルが通らなくても
//       コメントとして残し、JVM17 + エミュレータ環境で段階的に有効化する。
// ビルドを壊さないため、実装は全てコメントアウトで残す。必要に応じて
// Hilt / UIAutomator 依存を有効化してからコメントを外すこと。
// =============================================================================

// TODO(IMP-06) 6-4 UIAutomator 依存（app/build.gradle.kts に追記後にアンコメント）
// 実依存（コメント留め）:
// androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
// 既存の testImplementation / androidTestImplementation と併用。Hilt無しでも可だが
// 本プロジェクトは Hilt を使うため HiltTestRunner 経由で起動する。

// -----------------------------------------------------------------------------
// 有効化時にアンコメントする import 群
// -----------------------------------------------------------------------------
// import androidx.test.ext.junit.runners.AndroidJUnit4
// import androidx.test.platform.app.InstrumentationRegistry
// import androidx.test.uiautomator.UiDevice
// import androidx.test.uiautomator.UiSelector
// import androidx.test.uiautomator.UiObject
// import androidx.test.uiautomator.By
// import androidx.test.uiautomator.Until
// import androidx.test.rule.ActivityTestRule
// import androidx.test.filters.LargeTest
// import dagger.hilt.android.testing.HiltAndroidTest
// import dagger.hilt.android.testing.HiltAndroidRule
// import org.junit.Before
// import org.junit.Rule
// import org.junit.Test
// import org.junit.runner.RunWith
// import org.junit.Assert.*
// import com.plath.scancard.MainActivity
// import com.plath.scancard.data.local.AppDatabase
// import com.plath.scancard.domain.util.ExportManager
// import kotlinx.coroutines.test.runTest

// =============================================================================
// E2Eシナリオ: Deck作成 → スキャンMock → 抽出Mock → Studyフィルタ → Export検証
// 5% E2Eカバレッジ目標: 全テスト中の約1/20をE2Eが占める想定。まず1本を緑にする。
// =============================================================================

// // @HiltAndroidTest
// // @RunWith(AndroidJUnit4::class)
// // @LargeTest
// // class ScanToStudyE2ETest {
// //
// //     @get:Rule
// //     var hiltRule = HiltAndroidRule(this)
// //
// //     // UIAutomator は ActivityTestRule / ActivityScenario と併用可
// //     // @get:Rule
// //     // var activityRule = ActivityTestRule(MainActivity::class.java)
// //
// //     private lateinit var device: UiDevice
// //
// //     @Before
// //     fun setUp() {
// //         hiltRule.inject()
// //         device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
// //         // 初期状態: DBをクリア（FakeRepository or inMemory DB を Hilt で差し替え）
// //         // 例: hiltRule + TestModule で FakeGemmaExtractor / FakeDocumentScanner を注入
// //     }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 1: Happy path — Deck作成→スキャン→抽出→Study→Export
// //     // Maestroの scan_to_export_flow.yaml と対応。UIAutomatorで同フローを検証。
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun scanToStudyE2E_happyPath() {
// //     //     // 1. Launch app — home_flow.yaml の launchApp 相当
// //     //     // device.pressHome()
// //     //     // device.wait(Until.hasObject(By.text("ScanCard")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("ScanCard")))
// //     //
// //     //     // 2. Deck作成 — "Create Manual Deck" → input "Test Deck" → Create
// //     //     // val createDeckBtn = device.findObject(UiSelector().text("Create Manual Deck"))
// //     //     // createDeckBtn.click()
// //     //     // val input = device.findObject(UiSelector().className("android.widget.EditText"))
// //     //     // input.setText("E2E Test Deck")
// //     //     // device.findObject(UiSelector().text("Create")).click()
// //     //     // device.wait(Until.hasObject(By.text("E2E Test Deck")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("E2E Test Deck")))
// //     //
// //     //     // 3. スキャンMock — "Scan Document" FAB → Mock DocumentScanner
// //     //     //    Hilt TestModule で DocumentScannerManager を Fake に差し替え
// //     //     //    Fakeは固定の画像URIを返し、OCRはスキップ
// //     //     // val scanFab = device.findObject(UiSelector().description("Scan Document"))
// //     //     // scanFab.click()
// //     //     // device.wait(Until.hasObject(By.text("Scan Document")), 5000)
// //     //     // // Mock: FakeDocumentScanner が即座に "fake_image_uri" を返す
// //     //
// //     //     // 4. 抽出Mock — GemmaCardExtractor を Fake に差し替え、固定カードを返す
// //     //     //    例: listOf(Card(term="hello", definition="こんにちは"), Card(term="world", ...))
// //     //     // device.wait(Until.hasObject(By.text("Extraction Preview")), 10000)
// //     //     // device.findObject(UiSelector().text("Save")).click() // 抽出結果を保存
// //     //
// //     //     // 5. DeckDetail 検証 — 保存後に DeckDetail に遷移、カードが表示される
// //     //     // device.wait(Until.hasObject(By.text("hello")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("hello")))
// //     //     // assertTrue(device.hasObject(By.text("world")))
// //     //
// //     //     // 6. Study フィルタ — "Study" → Filter切替
// //     //     // device.findObject(UiSelector().text("Study")).click()
// //     //     // device.wait(Until.hasObject(By.text("Study")), 5000)
// //     //     // // FilterMenu: "Filter: ALL" → タップ → "LEARNED" 選択
// //     //     // device.findObject(UiSelector().textContains("Filter:")).click()
// //     //     // device.findObject(UiSelector().text("LEARNED")).click()
// //     //     // // フィルタ後のカード数検証（Mockで learned 0件なら "No cards to study" が表示）
// //     //     // // assertTrue(device.hasObject(By.text("No cards to study")))
// //     //
// //     //     // 7. Export TSV 検証 — Back → Export → TSV内容検証
// //     //     // device.pressBack()
// //     //     // device.findObject(UiSelector().text("Export")).click()
// //     //     // device.wait(Until.hasObject(By.textContains("Export")), 5000)
// //     //     // // ExportManager.exportToTSV の結果を検証（Clipboard or File）
// //     //     // // 真のファイル検証は `ExportManagerTest` (unit) で行い、E2Eでは UI上の成功トーストを確認
// //     //     // // assertTrue(device.hasObject(By.textContains("Exported")) || device.hasObject(By.textContains("Copied")))
// //     //
// //     //     // 8. クリーンアップ — 作成した Deck を削除
// //     //     // device.pressBack() // Homeに戻る
// //     //     // val deleteBtn = device.findObject(UiSelector().description("Delete"))
// //     //     // if (deleteBtn.exists()) { deleteBtn.click() }
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 2: Study フィルタ永続化 (IMP-07 Req9.6 と連携)
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun studyFilter_persistsAcrossSessions() {
// //     //     // 1. Study で filter を LEARNED に変更
// //     //     // 2. device.pressBack() → プロセスkill → 再起動（Hilt + DataStore 永続化を検証）
// //     //     // 3. 再度 Study に遷移し、filter が LEARNED のままであることを検証
// //     //     // DataStore 永続化は IMP-07 7-3 で実装予定。E2Eでは kill→relaunch を UiDevice で再現
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 3: WorkManager 通知検証 (IMP-07 7-4 と連携)
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun backgroundExtraction_notificationIsDisplayed() {
// //     //     // 1. スキャン → 抽出を WorkManager で実行（Fakeで遅延を挿入）
// //     //     // 2. device.openNotification() で通知ドロワーを開く
// //     //     // 3. device.wait(Until.hasObject(By.textContains("Extraction")), 15000)
// //     //     // 4. 通知タップでアプリに戻ることを検証
// //     //     // UIAutomator が必要な理由: Compose Test / Espresso では通知領域にアクセス不可
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 4: Edge-to-Edge 回帰（任意、IMP-03連携）
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun edgeToEdge_fabNotOverlappedByNavBar() {
// //     //     // device.setOrientationNatural()
// //     //     // val fab = device.findObject(UiSelector().description("Scan Document"))
// //     //     // assertTrue(fab.exists())
// //     //     // // FABの bounds が navigationBar 領域と重ならないことを検証（座標比較）
// //     // }
// // }

// =============================================================================
// 有効化手順
// =============================================================================
// 1. app/build.gradle.kts に追加:
//    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
//    androidTestImplementation("androidx.test:runner:1.7.0") // 既存
//    androidTestImplementation("androidx.test:rules:1.7.0")  // 既存
// 2. 本ファイルのコメントを外し、HiltTestRunner 設定が有効であることを確認
//    (app/build.gradle.kts:21 testInstrumentationRunner = "com.plath.scancard.HiltTestRunner")
// 3. Hilt TestModule で FakeDocumentScanner / FakeGemmaExtractor / inMemory DB を提供
//    （本番の MLKit / Room を使わず、Fakeで高速・安定化）
// 4. エミュレータ/実機で実行:
//    ./gradlew :app:connectedDebugAndroidTest --tests "com.plath.scancard.e2e.ScanToStudyE2ETest"
//    または特定シナリオのみ:
//    ./gradlew :app:connectedDebugAndroidTest --tests "*.scanToStudyE2E_happyPath"
// 5. 5% E2Eカバレッジ目標: 全テスト数を `find app/src/test -name "*Test.kt" | wc -l` で数え、
//    E2Eが約1/20になるように本クラスにシナリオを追加。最初は happyPath 1本を緑にする。
// =============================================================================

// NOTE: 本ファイルは雛形のため現状は全てコメント。androidTest sourceSet に配置しても
// ビルドに影響しない（コメントのみのためコンパイルエラーにならない）。

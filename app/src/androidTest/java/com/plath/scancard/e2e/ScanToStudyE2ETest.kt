package com.plath.scancard.e2e

// =============================================================================
// IMP-06 6-4 UIAutomator E2E Template (5% E2E coverage goal)
// =============================================================================
// Constraints: Full gradle execution and connectedDebugAndroidTest task are not available on JVM8.
//       This file is created as a "comment-based template". Even if it doesn't compile,
//       keep it as comments and gradually enable it after JVM17 + emulator environment.
// All implementations are commented out to avoid breaking the build. Enable by
// uncommenting after enabling Hilt / UIAutomator dependencies as needed.
// =============================================================================

// TODO(IMP-06) 6-4 UIAutomator dependency (Uncomment after adding to app/build.gradle.kts)
// Actual dependency (commented out):
// androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
// Can be used with existing testImplementation / androidTestImplementation. Hilt is not required,
// but as this project uses Hilt, it will be launched via HiltTestRunner.

// -----------------------------------------------------------------------------
// Imports to be uncommented upon activation
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
// E2E Scenario: Deck creation -> Scan Mock -> Extraction Mock -> Study Filter -> Export verification
// 5% E2E coverage goal: E2E tests are expected to account for about 1/20 of all tests. First, make one test green.
// =============================================================================

// // @HiltAndroidTest
// // @RunWith(AndroidJUnit4::class)
// // @LargeTest
// // class ScanToStudyE2ETest {
// //
// //     @get:Rule
// //     var hiltRule = HiltAndroidRule(this)
// //
// //     // UIAutomator can be used with ActivityTestRule / ActivityScenario
// //     // @get:Rule
// //     // var activityRule = ActivityTestRule(MainActivity::class.java)
// //
// //     private lateinit var device: UiDevice
// //
// //     @Before
// //     fun setUp() {
// //         hiltRule.inject()
// //         device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
// //         // Initial state: Clear DB (swap with FakeRepository or in-memory DB using Hilt)
// //         // Example: Inject FakeGemmaExtractor / FakeDocumentScanner via hiltRule + TestModule
// //     }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 1: Happy path — Deck creation -> Scan -> Extraction -> Study -> Export
// //     // Corresponds to scan_to_export_flow.yaml. Verify the same flow with UIAutomator.
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun scanToStudyE2E_happyPath() {
// //     //     // 1. Launch app — equivalent to launchApp in home_flow.yaml
// //     //     // device.pressHome()
// //     //     // device.wait(Until.hasObject(By.text("ScanCard")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("ScanCard")))
// //     //
// //     //     // 2. Deck creation — "Create Manual Deck" -> input "Test Deck" -> Create
// //     //     // val createDeckBtn = device.findObject(UiSelector().text("Create Manual Deck"))
// //     //     // createDeckBtn.click()
// //     //     // val input = device.findObject(UiSelector().className("android.widget.EditText"))
// //     //     // input.setText("E2E Test Deck")
// //     //     // device.findObject(UiSelector().text("Create")).click()
// //     //     // device.wait(Until.hasObject(By.text("E2E Test Deck")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("E2E Test Deck")))
// //     //
// //     //     // 3. Scan Mock — "Scan Document" FAB -> Mock DocumentScanner
// //     //     //    Swap DocumentScannerManager with Fake in Hilt TestModule
// //     //     //    Fake returns a fixed image URI and skips OCR
// //     //     // val scanFab = device.findObject(UiSelector().description("Scan Document"))
// //     //     // scanFab.click()
// //     //     // device.wait(Until.hasObject(By.text("Scan Document")), 5000)
// //     //     // // Mock: FakeDocumentScanner immediately returns "fake_image_uri"
// //     //
// //     //     // 4. Extraction Mock — Swap GemmaCardExtractor with Fake, return fixed cards
// //     //     //    Example: listOf(Card(term="hello", definition="hello_translated"), Card(term="world", ...))
// //     //     // device.wait(Until.hasObject(By.text("Extraction Preview")), 10000)
// //     //     // device.findObject(UiSelector().text("Save")).click() // Save extraction results
// //     //
// //     //     // 5. DeckDetail verification — transition to DeckDetail after saving, cards are displayed
// //     //     // device.wait(Until.hasObject(By.text("hello")), 5000)
// //     //     // assertTrue(device.hasObject(By.text("hello")))
// //     //     // assertTrue(device.hasObject(By.text("world")))
// //     //
// //     //     // 6. Study Filter — "Study" -> Filter switch
// //     //     // device.findObject(UiSelector().text("Study")).click()
// //     //     // device.wait(Until.hasObject(By.text("Study")), 5000)
// //     //     // // FilterMenu: "Filter: ALL" -> tap -> select "LEARNED"
// //     //     // device.findObject(UiSelector().textContains("Filter:")).click()
// //     //     // device.findObject(UiSelector().text("LEARNED")).click()
// //     //     // // Verify card count after filtering (if 0 learned cards in Mock, "No cards to study" is displayed)
// //     //     // // assertTrue(device.hasObject(By.text("No cards to study")))
// //     //
// //     //     // 7. Export TSV verification — Back -> Export -> verify TSV content
// //     //     // device.pressBack()
// //     //     // device.findObject(UiSelector().text("Export")).click()
// //     //     // device.wait(Until.hasObject(By.textContains("Export")), 5000)
// //     //     // // Verify results of ExportManager.exportToTSV (Clipboard or File)
// //     //     // // Actual file verification is done in ExportManagerTest (unit); in E2E, verify success toast on UI.
// //     //     // // assertTrue(device.hasObject(By.textContains("Exported")) || device.hasObject(By.textContains("Copied")))
// //     //
// //     //     // 8. Cleanup — delete the created Deck
// //     //     // device.pressBack() // Back to Home
// //     //     // val deleteBtn = device.findObject(UiSelector().description("Delete"))
// //     //     // if (deleteBtn.exists()) { deleteBtn.click() }
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 2: Study filter persistence (Linked with IMP-07 Req9.6)
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun studyFilter_persistsAcrossSessions() {
// //     //     // 1. Change filter to LEARNED in Study
// //     //     // 2. device.pressBack() -> process kill -> restart (verify Hilt + DataStore persistence)
// //     //     // 3. Transition to Study again and verify filter remains LEARNED
// //     //     // DataStore persistence is planned for IMP-07 7-3. Reproduce kill->relaunch with UiDevice in E2E.
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 3: WorkManager notification verification (Linked with IMP-07 7-4)
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun backgroundExtraction_notificationIsDisplayed() {
// //     //     // 1. Scan -> Run extraction with WorkManager (insert delay with Fake)
// //     //     // 2. Open notification drawer with device.openNotification()
// //     //     // 3. device.wait(Until.hasObject(By.textContains("Extraction")), 15000)
// //     //     // 4. Verify return to app by tapping notification
// //     //     // Why UIAutomator is needed: Notification area is inaccessible in Compose Test / Espresso.
// //     // }
// //
// //     // -------------------------------------------------------------------------
// //     // Scenario 4: Edge-to-Edge regression (Optional, Linked with IMP-03)
// //     // -------------------------------------------------------------------------
// //     // @Test
// //     // fun edgeToEdge_fabNotOverlappedByNavBar() {
// //     //     // device.setOrientationNatural()
// //     //     // val fab = device.findObject(UiSelector().description("Scan Document"))
// //     //     // assertTrue(fab.exists())
// //     //     // // Verify that FAB bounds do not overlap with navigationBar area (coordinate comparison)
// //     // }
// // }

// =============================================================================
// Activation Procedure
// =============================================================================
// 1. Add to app/build.gradle.kts:
//    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
//    androidTestImplementation("androidx.test:runner:1.7.0") // Existing
//    androidTestImplementation("androidx.test:rules:1.7.0")  // Existing
// 2. Uncomment this file and confirm HiltTestRunner setting is enabled
//    (app/build.gradle.kts:21 testInstrumentationRunner = "com.plath.scancard.HiltTestRunner")
// 3. Provide FakeDocumentScanner / FakeGemmaExtractor / in-memory DB in Hilt TestModule
//    (Faster and more stable with Fakes, without using production MLKit / Room)
// 4. Run on emulator/device:
//    `./gradlew :app:connectedDebugAndroidTest --tests "com.plath.scancard.e2e.ScanToStudyE2ETest"`
//    Or for specific scenario only:
//    `./gradlew :app:connectedDebugAndroidTest --tests "*.scanToStudyE2E_happyPath"`
// 5. 5% E2E coverage goal: Count all tests with `find app/src/test -name "*Test.kt" | wc -l`,
//    and add scenarios to this class so E2E is about 1/20. First, make one happyPath test green.
// =============================================================================

// NOTE: This file is a template and currently all comments. It doesn't affect build even if placed
// in androidTest sourceSet (no compilation errors as it's only comments).

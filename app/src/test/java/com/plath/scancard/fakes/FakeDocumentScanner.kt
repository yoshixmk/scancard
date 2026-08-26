package com.plath.scancard.fakes

import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * FakeDocumentScanner — IMP-10 10-4
 *
 * Purpose: Test asynchronous lifecycles of ScanDocumentUseCase / ScanViewModel using Fakes instead of Mockito.
 * Adheres to: `.kiro/skills/camerax/references/testing.md` Fakes over mocks / Google Truth / Explicit @RunWith
 *
 * The actual DocumentScannerManager depends on Play Services (GmsDocumentScanning.getClient) and
 * cannot be initialized in Robolectric / JVM unit tests. This Fake does not call Play Services at all
 * and simulates scan results in memory.
 *
 * Usage:
 * ```
 * @RunWith(AndroidJUnit4::class) // or RobolectricTestRunner
 * class ScanDocumentUseCaseTest {
 *     private val fakeScanner = FakeDocumentScanner()
 *     private val fakeRecognition = FakeTextRecognition() // Separate Fake
 *     private lateinit var useCase: ScanDocumentUseCase
 *
 *     @Before fun setUp() {
 *         fakeScanner.setNextResult(listOf(mockUri1, mockUri2)) // Success case
 *         // Scanner itself doesn't directly depend on UseCase, so swap at the ViewModel layer
 *         // useCase = ScanDocumentUseCase(fakeRecognition, fakeRepo) 
 *     }
 *
 *     @Test fun scanSuccess_addsPages() {
 *         val result = fakeScanner.simulateScanSuccess()
 *         assertThat(result.pages).hasSize(2) // Truth
 *     }
 * }
 * ```
 *
 * Design Principles:
 * - Prioritize state verification (verify state) over Mockito's behavior verification (verify(behavior)).
 * - Verify asynchronous operations using kotlinx-coroutines-test's runTest + Turbine instead of CountDownLatch / IdlingResource.
 * - ScanScreen's ActivityResultLauncher vicinity is not reproduced in Fake — test ViewModel's addPages/removePage/processScans directly.
 *
 * Upon future CameraX migration, use with FakeCameraConfig (androidx.camera:camera-testing) and
 * replace ProcessCameraProvider.awaitInstance(context) initialization with Fake (see testing.md).
 */

// Lightweight alternative to GmsDocumentScanningResult — as the actual GmsDocumentScanningResult is final and hard to mock, a Fake DTO is provided
data class FakeScanPage(val imageUri: Uri)

data class FakeScanResult(
    val pages: List<FakeScanPage>,
    val pdfUri: Uri? = null
)

/**
 * FakeDocumentScanner — Fake replacement for DocumentScannerManager
 *
 * Instead of the actual DocumentScannerManager.createLauncher / startScan,
 * success/failure/cancellation can be triggered directly from the test code.
 */
class FakeDocumentScanner {

    // --- Configurable behaviors (Manipulate in test's given section) ---

    // URIs to return in the next scan. Tests can treat empty lists as cancellations.
    private var nextUris: List<Uri> = emptyList()

    // Set to non-null to simulate an error
    private var nextError: Exception? = null

    // Flag to simulate cancellation
    private var nextCanceled: Boolean = false

    // For verifying call history
    var startScanCallCount: Int = 0
        private set
    var lastLaunchedUris: List<Uri>? = null
        private set

    // Callback capture (For Escaping verification — the actual one escapes to ActivityResultLauncher)
    var onSuccessInvoked: Boolean = false
        private set
    var onErrorInvoked: Boolean = false
        private set
    var onCanceledInvoked: Boolean = false
        private set

    // --- Operation APIs from tests ---

    /** Set URIs for success in the next scan */
    fun setNextResult(uris: List<Uri>) {
        nextUris = uris
        nextError = null
        nextCanceled = false
    }

    /** Set to return an error in the next scan */
    fun setNextError(exception: Exception) {
        nextError = exception
        nextCanceled = false
    }

    /** Set to return cancellation in the next scan */
    fun setNextCanceled() {
        nextCanceled = true
        nextError = null
    }

    // --- Fake implementation of actual DocumentScannerManager.startScan / createLauncher ---

    /**
     * Fake startScan — synchronously simulates actual scanner.getStartScanIntent(activity).addOnSuccessListener
     * equivalent. Directly calls callbacks without generating an actual IntentSender.
     *
     * @param onSuccess Returns FakeScanResult on success
     * @param onError On failure
     * @param onCanceled On cancellation
     */
    fun startScan(
        onSuccess: (FakeScanResult) -> Unit,
        onError: (Exception) -> Unit,
        onCanceled: () -> Unit
    ) {
        startScanCallCount++
        when {
            nextCanceled -> {
                onCanceledInvoked = true
                onCanceled()
            }
            nextError != null -> {
                onErrorInvoked = true
                onError(nextError!!)
            }
            else -> {
                onSuccessInvoked = true
                val result = FakeScanResult(pages = nextUris.map { FakeScanPage(it) })
                lastLaunchedUris = nextUris
                onSuccess(result)
            }
        }
    }

    /**
     * Simulates ScanScreen's scannerLauncher equivalent — helper that directly returns a URI list
     * instead of ActivityResultContracts.StartIntentSenderForResult. Used for testing ViewModel.addPages().
     */
    fun simulateScanSuccess(): FakeScanResult {
        // Defaults to value set via setNextResult, or empty if not set
        return FakeScanResult(pages = nextUris.map { FakeScanPage(it) })
    }

    fun simulateScanError(): Exception = nextError ?: Exception("Fake scan error")

    fun simulateCancel(): Boolean = nextCanceled

    /** Reset state between tests — call in @Before / @After */
    fun reset() {
        nextUris = emptyList()
        nextError = null
        nextCanceled = false
        startScanCallCount = 0
        lastLaunchedUris = null
        onSuccessInvoked = false
        onErrorInvoked = false
        onCanceledInvoked = false
    }

    // --- Truth assertions substitution examples (see docs/testing.md 10-5) ---
    // JUnit: assertEquals(2, result.pages.size)
    // Truth: assertThat(result.pages).hasSize(2)
    // Truth: assertThat(onSuccessInvoked).isTrue()
    // Truth: assertThat(lastLaunchedUris).containsExactly(uri1, uri2)
}

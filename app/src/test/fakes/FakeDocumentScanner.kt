package com.example.scancard.fakes

import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * FakeDocumentScanner — IMP-10 10-4
 *
 * 目的: Mockito ではなく Fake で ScanDocumentUseCase / ScanViewModel の非同期ライフサイクルをテストする。
 * 準拠: `.kiro/skills/camerax/references/testing.md` Fakes over mocks / Google Truth / Explicit @RunWith
 *
 * 本物の DocumentScannerManager は Play Services (GmsDocumentScanning.getClient) に依存し
 * Robolectric / JVM ユニットテストで初期化不可。本 Fake は Play Services を一切呼ばず、
 * メモリ上でスキャン結果をシミュレートする。
 *
 * 使い方:
 * ```
 * @RunWith(AndroidJUnit4::class) // または RobolectricTestRunner
 * class ScanDocumentUseCaseTest {
 *     private val fakeScanner = FakeDocumentScanner()
 *     private val fakeRecognition = FakeTextRecognition() // 別Fake
 *     private lateinit var useCase: ScanDocumentUseCase
 *
 *     @Before fun setUp() {
 *         fakeScanner.setNextResult(listOf(mockUri1, mockUri2)) // 成功ケース
 *         // useCase = ScanDocumentUseCase(fakeRecognition, fakeRepo) // Scanner自体はUseCaseに直接依存しないためViewModel層で差替
 *     }
 *
 *     @Test fun scanSuccess_addsPages() {
 *         val result = fakeScanner.simulateScanSuccess()
 *         assertThat(result.pages).hasSize(2) // Truth
 *     }
 * }
 * ```
 *
 * 設計原則:
 * - 状態検証 (verify state) を優先し、Mockito の verify(behavior) は使わない。
 * - 非同期は CountDownLatch / IdlingResource ではなく kotlinx-coroutines-test の runTest + Turbine で検証。
 * - ScanScreen の ActivityResultLauncher 周りは Fake では再現しない — ViewModel の addPages/removePage/processScans を直接テスト。
 *
 * 将来 CameraX 移行時は FakeCameraConfig (androidx.camera:camera-testing) と併用し、
 * ProcessCameraProvider.awaitInstance(context) の初期化を Fake で置換する (testing.md 参照)。
 */

// 軽量な GmsDocumentScanningResult の代替 — 本物の GmsDocumentScanningResult は final でモック困難なため Fake DTO を用意
data class FakeScanPage(val imageUri: Uri)

data class FakeScanResult(
    val pages: List<FakeScanPage>,
    val pdfUri: Uri? = null
)

/**
 * FakeDocumentScanner — DocumentScannerManager の Fake 置換
 *
 * 本物の DocumentScannerManager.createLauncher / startScan の代わりに、
 * テストコードから直接成功/失敗/キャンセルをトリガできる。
 */
class FakeDocumentScanner {

    // --- 設定可能な振る舞い (テストの given で操作) ---

    // 次回スキャンで返す Uri 群。空ならキャンセル扱いにするテストも可能。
    private var nextUris: List<Uri> = emptyList()

    // エラーをシミュレートする場合は non-null にする
    private var nextError: Exception? = null

    // キャンセルをシミュレートするフラグ
    private var nextCanceled: Boolean = false

    // 呼び出し履歴の検証用
    var startScanCallCount: Int = 0
        private set
    var lastLaunchedUris: List<Uri>? = null
        private set

    // コールバックのキャプチャ (Escaping 検証用 — 本物では ActivityResultLauncher にエスケープする)
    var onSuccessInvoked: Boolean = false
        private set
    var onErrorInvoked: Boolean = false
        private set
    var onCanceledInvoked: Boolean = false
        private set

    // --- テストからの操作 API ---

    /** 次回スキャンで成功する Uri 列を設定 */
    fun setNextResult(uris: List<Uri>) {
        nextUris = uris
        nextError = null
        nextCanceled = false
    }

    /** 次回スキャンでエラーを返すように設定 */
    fun setNextError(exception: Exception) {
        nextError = exception
        nextCanceled = false
    }

    /** 次回スキャンでキャンセルを返すように設定 */
    fun setNextCanceled() {
        nextCanceled = true
        nextError = null
    }

    // --- 本物の DocumentScannerManager.startScan / createLauncher の Fake 実装 ---

    /**
     * Fake の startScan — 本物の scanner.getStartScanIntent(activity).addOnSuccessListener 相当を
     * 同期的にシミュレート。実際の IntentSender は生成せず、コールバックを直接呼ぶ。
     *
     * @param onSuccess 成功時に FakeScanResult を返す
     * @param onError 失敗時
     * @param onCanceled キャンセル時
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
     * ScanScreen の scannerLauncher 相当をシミュレート — ActivityResultContracts.StartIntentSenderForResult の代わりに
     * 直接 Uri リストを返すヘルパ。ViewModel.addPages() のテストに利用。
     */
    fun simulateScanSuccess(): FakeScanResult {
        // デフォルトは setNextResult で設定された値、未設定なら空
        return FakeScanResult(pages = nextUris.map { FakeScanPage(it) })
    }

    fun simulateScanError(): Exception = nextError ?: Exception("Fake scan error")

    fun simulateCancel(): Boolean = nextCanceled

    /** テスト間の状態リセット — @Before / @After で呼ぶ */
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

    // --- Truth assertions 置換例 (docs/testing.md 10-5 参照) ---
    // JUnit: assertEquals(2, result.pages.size)
    // Truth: assertThat(result.pages).hasSize(2)
    // Truth: assertThat(onSuccessInvoked).isTrue()
    // Truth: assertThat(lastLaunchedUris).containsExactly(uri1, uri2)
}

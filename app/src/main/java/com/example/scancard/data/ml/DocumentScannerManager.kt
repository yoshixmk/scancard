package com.example.scancard.data.ml

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * IMP-10 10-1 監査結果 (2026-08-23) — camerax/SKILL.md immutability / thermals / threading 照合
 *
 * [スレッド] OK: GmsDocumentScanning.getClient(options).getStartScanIntent は Play Services Task を返す。
 *   addOnSuccessListener / addOnFailureListener は MainThread で実行されるため ScannerLauncher 起動は
 *   UIスレッドで安全。ただし ScanScreen.kt:68 の scanner も Main で remember 生成されているため競合なし。
 *   将来 CameraX に移行する場合 (ProcessCameraProvider.getInstance) は ListenableFuture.await() で
 *   Dispatchers.Main で await する必要あり — 本クラスでは不要。
 *
 * [Escaping] 要注意: createLauncher() の onSuccess/onError/onCanceled ラムダは
 *   ActivityResultLauncher にキャプチャされ Activity 破棄まで生存 (Escaping closure)。
 *   ComponentActivity#registerForActivityResult は Lifecycle に紐づくため
 *   Activity再生成時に再登録が必要。ViewModel で保持せず毎回 createLauncher 呼び出しは正しいが、
 *   呼び出し元で launcher を ViewModel に保持しないこと。ScanScreen.kt は composition 内 remember なので OK。
 *   改善案: onError 未伝播 (startScan の addOnFailureListener が空) — 呼び出し元に onError を通知すべき。
 *
 * [Immutability / Builder再代入] OK: GmsDocumentScannerOptions.Builder は fluent だが
 *   本コードはチェーンメソッド -> build() で正しく再代入(チェーン)している。
 *   camerax/SKILL.md immutability.md の PendingRecording.withAudioEnabled() のような
 *   "戻り値を捨てる" 漏れはなし。pageLimit=100 は ML Kit上限内の固定値で再代入漏れなし。
 *   将来 GmsDocumentScannerOptions に NightMode 等が追加された場合も Builder チェーンを維持すること。
 *
 * [Thermal / リソース] N/A: DocumentScanner は Play Services 側で管理、StreamUseCase 非適用。
 *   熱対策が必要な場合は CameraX Preview + ImageAnalysis に移行後に docs/camera-thermals.md 参照。
 *
 * [残課題] startScan の addOnFailureListener で // Handle error コメントのみ — onError コールバックに
 *   伝播するか Log + Snackbar を出すべき。Audit時点では既存動作を壊さないためコメントのみ追記。
 */
class DocumentScannerManager @Inject constructor() {
    private val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setPageLimit(100)
        .build()

    private val scanner = GmsDocumentScanning.getClient(options)

    fun createLauncher(
        activity: ComponentActivity,
        onSuccess: (GmsDocumentScanningResult) -> Unit,
        onError: (Exception) -> Unit,
        onCanceled: () -> Unit
    ): ActivityResultLauncher<IntentSenderRequest> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                if (scanResult != null) {
                    onSuccess(scanResult)
                } else {
                    onError(Exception("Failed to get scan result from intent"))
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                onCanceled()
            }
        }
    }

    fun startScan(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}

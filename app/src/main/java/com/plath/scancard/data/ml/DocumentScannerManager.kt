package com.plath.scancard.data.ml

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
 * IMP-10 10-1 Audit results (2026-08-23) — Comparison with camerax/SKILL.md immutability / thermals / threading
 *
 * [Threading] OK: GmsDocumentScanning.getClient(options).getStartScanIntent returns a Play Services Task.
 *   Since addOnSuccessListener / addOnFailureListener are executed on MainThread, ScannerLauncher launch is
 *   safe on UI thread. Also, as scanner in ScanScreen.kt:68 is remember-generated on Main, there's no conflict.
 *   When migrating to CameraX (ProcessCameraProvider.getInstance) in the future, ListenableFuture.await()
 *   needs to be awaited on Dispatchers.Main — not required in this class.
 *
 * [Escaping] Note: onSuccess/onError/onCanceled lambdas in createLauncher() are
 *   captured by ActivityResultLauncher and survive until Activity destruction (Escaping closure).
 *   Re-registration is required upon Activity recreation. Calling createLauncher every time instead
 *   of holding in ViewModel is correct, but don't hold the launcher in ViewModel on the caller side.
 *   ScanScreen.kt is OK as it uses remember within composition.
 *   Improvement proposal: onError not propagated (addOnFailureListener in startScan is empty) —
 *   should notify caller of onError. Only comments added at the time of Audit to avoid breaking
 *   existing behavior.
 *
 * [Immutability / Builder reassignment] OK: GmsDocumentScannerOptions.Builder is fluent, but
 *   this code correctly reassigns (chains) via method chain -> build().
 *   No omissions like "discarding return value" in PendingRecording.withAudioEnabled()
 *   from camerax/SKILL.md immutability.md. pageLimit=100 is a fixed value within ML Kit limits;
 *   no reassignment omissions. Maintain Builder chain even if NightMode etc. are added
 *   to GmsDocumentScannerOptions in the future.
 *
 * [Thermal / Resources] N/A: DocumentScanner is managed on the Play Services side; StreamUseCase
 *   not applied. See docs/camera-thermals.md after migrating to CameraX Preview + ImageAnalysis
 *   if thermal measures are needed.
 *
 * [Remaining issues] Only "// Handle error" comment in addOnFailureListener of startScan — should
 *   propagate to onError callback or show Log + Snackbar. Only comments added at the time of Audit
 *   to avoid breaking existing behavior.
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

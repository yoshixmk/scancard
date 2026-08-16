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

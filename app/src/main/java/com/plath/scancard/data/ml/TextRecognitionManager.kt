package com.plath.scancard.data.ml

import android.net.Uri
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class MlKitModelNotReadyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * TextRecognitionManager — ML Kit Text Recognition (unbundled, Japanese).
 * Uses play-services-mlkit-text-recognition + text-recognition-japanese via Play Services
 * (dynamic download, no bundled model). Supports Japanese vertical/horizontal + English mixed.
 * Offline after model cached; retries once via ModuleInstall if model not ready.
 */
@Singleton
class TextRecognitionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = loadBitmapScaled(context, imageUri, 1080)
        val image = if (bitmap != null) InputImage.fromBitmap(bitmap, 0) else InputImage.fromFilePath(context, imageUri)
        recognizeInternal(image)
    }

    suspend fun recognizeTextFromBitmap(bitmap: android.graphics.Bitmap): String = withContext(Dispatchers.IO) {
        val scaled = downscaleIfNeeded(bitmap, 1080)
        val image = InputImage.fromBitmap(scaled, 0)
        recognizeInternal(image)
    }

    private suspend fun recognizeInternal(image: InputImage): String {
        try {
            return recognizer.process(image).await().text
        } catch (e: Exception) {
            if (isModelNotReady(e)) {
                try {
                    val moduleInstall = ModuleInstall.getClient(context)
                    val request = ModuleInstallRequest.newBuilder()
                        .addApi(TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()))
                        .build()
                    moduleInstall.installModules(request).await()
                    return recognizer.process(image).await().text
                } catch (retryEx: Exception) {
                    throw MlKitModelNotReadyException("Japanese OCR model not ready after retry", retryEx)
                }
            } else {
                throw e
            }
        }
    }

    private fun isModelNotReady(e: Exception): Boolean {
        val msg = (e.message ?: "") + " " + (e.cause?.message ?: "")
        return msg.contains("model", ignoreCase = true) ||
            msg.contains("not ready", ignoreCase = true) ||
            msg.contains("waiting for model", ignoreCase = true) ||
            e.javaClass.simpleName.contains("MlKitException")
    }

    private fun downscaleIfNeeded(bitmap: android.graphics.Bitmap, maxLongEdge: Int): android.graphics.Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxLongEdge && h <= maxLongEdge) return bitmap
        val ratio = maxLongEdge.toFloat() / maxOf(w, h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun loadBitmapScaled(context: Context, uri: Uri, maxLongEdge: Int): android.graphics.Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val original = android.graphics.BitmapFactory.decodeStream(input) ?: return null
                val scaled = downscaleIfNeeded(original, maxLongEdge)
                if (scaled !== original) original.recycle()
                scaled
            }
        } catch (_: Exception) { null }
    }

    fun close() { try { recognizer.close() } catch (_: Exception) {} }
}

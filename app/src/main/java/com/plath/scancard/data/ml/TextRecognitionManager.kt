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
        val t0 = android.os.SystemClock.elapsedRealtime()
        val tWall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        android.util.Log.d("TextRecog", "recognizeText start wall=$tWall uri=$imageUri")
        val bitmap = loadBitmapScaled(context, imageUri, 1080)
        val tLoad = android.os.SystemClock.elapsedRealtime() - t0
        val image = if (bitmap != null) InputImage.fromBitmap(bitmap, 0) else InputImage.fromFilePath(context, imageUri)
        val text = recognizeInternal(image)
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("TextRecog", "recognizeText done wall=$tWall uri=$imageUri load=${tLoad}ms total=${total}ms len=${text.length}")
        text
    }

    suspend fun recognizeTextFromBitmap(bitmap: android.graphics.Bitmap): String = withContext(Dispatchers.IO) {
        val t0 = android.os.SystemClock.elapsedRealtime()
        val tWall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        android.util.Log.d("TextRecog", "recognizeTextFromBitmap start wall=$tWall size=${bitmap.width}x${bitmap.height}")
        val scaled = downscaleIfNeeded(bitmap, 1080)
        val tScale = android.os.SystemClock.elapsedRealtime() - t0
        val image = InputImage.fromBitmap(scaled, 0)
        val text = recognizeInternal(image)
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("TextRecog", "recognizeTextFromBitmap done wall=$tWall scale=${tScale}ms total=${total}ms orig=${bitmap.width}x${bitmap.height} scaled=${scaled.width}x${scaled.height} len=${text.length}")
        text
    }

    private suspend fun recognizeInternal(image: InputImage): String {
        val t0 = android.os.SystemClock.elapsedRealtime()
        try {
            val text = recognizer.process(image).await().text
            val dt = android.os.SystemClock.elapsedRealtime() - t0
            android.util.Log.d("TextRecog", "recognizeInternal success took=${dt}ms len=${text.length}")
            return text
        } catch (e: Exception) {
            val dt = android.os.SystemClock.elapsedRealtime() - t0
            android.util.Log.w("TextRecog", "recognizeInternal failed after ${dt}ms: ${e.message}", e)
            if (isModelNotReady(e)) {
                try {
                    val tInst0 = android.os.SystemClock.elapsedRealtime()
                    android.util.Log.d("TextRecog", "Model not ready -> ModuleInstall request")
                    val moduleInstall = ModuleInstall.getClient(context)
                    val request = ModuleInstallRequest.newBuilder()
                        .addApi(TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()))
                        .build()
                    moduleInstall.installModules(request).await()
                    val tInst = android.os.SystemClock.elapsedRealtime() - tInst0
                    android.util.Log.d("TextRecog", "ModuleInstall done took=${tInst}ms, retrying recognizer")
                    val tRetry0 = android.os.SystemClock.elapsedRealtime()
                    val text = recognizer.process(image).await().text
                    val tRetry = android.os.SystemClock.elapsedRealtime() - tRetry0
                    android.util.Log.d("TextRecog", "recognizeInternal retry success took=${tRetry}ms len=${text.length}")
                    return text
                } catch (retryEx: Exception) {
                    android.util.Log.e("TextRecog", "ModuleInstall retry failed", retryEx)
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

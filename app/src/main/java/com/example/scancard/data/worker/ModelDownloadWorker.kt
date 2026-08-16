package com.example.scancard.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "ModelDownloadWorker"

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: return Result.failure()
        val authToken = inputData.getString("authToken") ?: return Result.failure()

        Log.d(TAG, "Starting download for $fileName from $url")

        // Interceptor to handle redirects while preserving Authorization header
        // Note: OkHttp by default strips Auth header on redirect to different host for security.
        // Hugging Face redirects to CloudFront/S3, so we need to re-add it carefully.
        val client = OkHttpClient.Builder()
            .followRedirects(false) // Handle redirects manually
            .build()

        var currentUrl = url
        var response: Response? = null
        
        try {
            // Loop for handling redirects (max 5)
            var redirectCount = 0
            while (redirectCount < 5) {
                val requestBuilder = Request.Builder().url(currentUrl)
                
                // Only add Bearer token if we are on the primary Hugging Face domain.
                // Pre-signed CDN/S3 URLs usually already have auth in query params and will fail if we include it.
                if (currentUrl.startsWith("https://huggingface.co") && !currentUrl.contains("cdn-lfs")) {
                    requestBuilder.addHeader("Authorization", "Bearer $authToken")
                }
                
                response = client.newCall(requestBuilder.build()).execute()
                
                if (response.isRedirect) {
                    val location = response.header("Location") ?: break
                    // Safely resolve relative redirects using OkHttp's HttpUrl.resolve()
                    val nextUrl = response.request.url.resolve(location)
                    if (nextUrl == null) {
                        Log.e(TAG, "Failed to resolve redirect location: $location")
                        break
                    }
                    currentUrl = nextUrl.toString()
                    response.close()
                    redirectCount++
                    Log.d(TAG, "Redirecting to: $currentUrl")
                } else {
                    break
                }
            }

            val finalResponse = response ?: return Result.failure()
            
            if (!finalResponse.isSuccessful) {
                Log.e(TAG, "Server returned error: ${finalResponse.code} ${finalResponse.message}")
                return Result.failure()
            }

            val body = finalResponse.body ?: return Result.failure()
            val file = File(applicationContext.filesDir, fileName)
            val totalBytes = body.contentLength()
            
            Log.d(TAG, "Content length: $totalBytes bytes")

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = totalRead.toFloat() / totalBytes
                            setProgress(workDataOf("progress" to progress))
                        }
                    }
                }
            }
            Log.d(TAG, "Download completed successfully: $fileName")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed with exception", e)
            return Result.failure()
        } finally {
            response?.close()
        }
    }
}

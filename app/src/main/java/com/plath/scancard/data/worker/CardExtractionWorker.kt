package com.plath.scancard.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.usecase.ExtractCardsUseCase
import com.plath.scancard.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CardExtractionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val extractCardsUseCase: ExtractCardsUseCase,
    private val deckRepository: DeckRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val deckId = inputData.getLong("deckId", -1)
        val notification = notificationHelper.createForegroundNotification(deckId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // dataSync: Changed because shortService has a ~3-minute hard limit and actual
            // extraction (several minutes) might be killed midway (Req12.6)
            ForegroundInfo(
                deckId.toInt(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(deckId.toInt(), notification)
        }
    }

    override suspend fun doWork(): Result {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        val deckId = inputData.getLong("deckId", -1)
        val modelId = inputData.getString("modelId") ?: ModelConfig.DEFAULT_ID
        android.util.Log.d(TAG, "doWork start wall=$wall deck=$deckId model=$modelId attempt=$runAttemptCount")

        if (deckId == -1L) return Result.failure()

        // On MIUI / Android 14+, SystemJobService performs onStopJob in 10 seconds,
        // so promote to foreground immediately after starting to prevent being killed.
        val tFg0 = android.os.SystemClock.elapsedRealtime()
        try {
            setForeground(getForegroundInfo())
            val tFg = android.os.SystemClock.elapsedRealtime() - tFg0
            android.util.Log.d(TAG, "setForeground done took=${tFg}ms wall=$wall deck=$deckId")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "setForeground failed after ${android.os.SystemClock.elapsedRealtime()-tFg0}ms wall=$wall", e)
        }

        val tStatus0 = android.os.SystemClock.elapsedRealtime()
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.RUNNING)
        android.util.Log.d(TAG, "update RUNNING done took=${android.os.SystemClock.elapsedRealtime()-tStatus0}ms wall=$wall")

        val modelConfig = ModelConfig.AVAILABLE_MODELS.find { it.id == modelId }
            ?: ModelConfig.GEMMA_4_E2B

        return try {
            val tExtract0 = android.os.SystemClock.elapsedRealtime()
            extractCardsUseCase.extractAndSaveCards(deckId, modelConfig) { current, total ->
                // Progress mirror (Req12.14) + notification area display (Req12.12).
                // Post progress notification with a different app management ID (if the
                // same ID is used, WM reposts and overwrites the original FGS notification)
                android.util.Log.d(TAG, "progress $current/$total wall=$wall deck=$deckId")
                setProgress(
                    androidx.work.workDataOf(
                        "progress_current" to current,
                        "progress_total" to total
                    )
                )
                notificationHelper.showProgressNotification(deckId, current, total)
            }
            val tExtract = android.os.SystemClock.elapsedRealtime() - tExtract0
            android.util.Log.d(TAG, "extractAndSaveCards done took=${tExtract}ms wall=$wall deck=$deckId")
            val tNotify0 = android.os.SystemClock.elapsedRealtime()
            notificationHelper.showCompletionNotification(deckId)
            val tNotify = android.os.SystemClock.elapsedRealtime() - tNotify0
            val total = android.os.SystemClock.elapsedRealtime() - t0
            android.util.Log.d(TAG, "doWork success wall=$wall deck=$deckId total=${total}ms extract=${tExtract}ms notify=${tNotify}ms")
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Rethrow to maintain WorkManager's stop/cancel semantics (Req12.10).
            // Converting to failure() broke automatic rescheduling after system stop,
            // causing "App restart -> job cancel -> failure".
            android.util.Log.w(TAG, "Work cancelled for deck $deckId — deferring to WorkManager retry", e)
            throw e
        } catch (e: Exception) {
            // Retry temporary errors up to 3 times with backoff; FAILED + notification only upon permanent failure (Req12.9/12.11)
            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                android.util.Log.w(
                    TAG,
                    "Extraction attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS failed for deck $deckId — retrying",
                    e
                )
                Result.retry()
            } else {
                android.util.Log.e(TAG, "Extraction permanently failed for deck $deckId", e)
                deckRepository.updateExtractionStatus(deckId, ExtractionStatus.FAILED)
                notificationHelper.showErrorNotification(deckId, e.message ?: "Unknown error")
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "CardExtractionWorker"
        private const val MAX_ATTEMPTS = 3
    }
}

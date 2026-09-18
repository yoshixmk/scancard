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
        val deckId = inputData.getLong("deckId", -1)
        val modelId = inputData.getString("modelId") ?: ModelConfig.DEFAULT_ID
        val scanIds = inputData.getLongArray("scanIds")?.toList() ?: emptyList()

        if (deckId == -1L) return Result.failure()

        // On MIUI / Android 14+, SystemJobService performs onStopJob in 10 seconds,
        // so promote to foreground immediately after starting to prevent being killed.
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "setForeground failed", e)
        }

        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.RUNNING)

        val modelConfig = ModelConfig.AVAILABLE_MODELS.find { it.id == modelId }
            ?: ModelConfig.GEMMA_4_E2B

        return try {
            extractCardsUseCase.extractAndSaveCards(deckId, modelConfig, scanIds) { current, total ->
                // Progress mirror (Req12.14) + notification area display (Req12.12).
                // Post progress notification with a different app management ID (if the
                // same ID is used, WM reposts and overwrites the original FGS notification)
                setProgress(
                    androidx.work.workDataOf(
                        "progress_current" to current,
                        "progress_total" to total
                    )
                )
                notificationHelper.showProgressNotification(deckId, current, total)
            }
            notificationHelper.showCompletionNotification(deckId)
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

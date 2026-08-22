package com.example.scancard.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.usecase.ExtractCardsUseCase
import com.example.scancard.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CardExtractionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val extractCardsUseCase: ExtractCardsUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deckId = inputData.getLong("deckId", -1)
        val modelId = inputData.getString("modelId") ?: "gemma-4-e2b"
        
        if (deckId == -1L) return Result.failure()
        
        val modelConfig = ModelConfig.AVAILABLE_MODELS.find { it.id == modelId } 
            ?: ModelConfig.GEMMA_4_E2B

        return try {
            extractCardsUseCase.extractAndSaveCards(deckId, modelConfig)
            notificationHelper.showCompletionNotification(deckId)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CardExtractionWorker", "Error during background extraction", e)
            notificationHelper.showErrorNotification(deckId, e.message ?: "Unknown error")
            Result.retry()
        }
    }
}

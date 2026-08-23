package com.plath.scancard.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.usecase.ExtractCardsUseCase
import com.plath.scancard.util.NotificationHelper
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Manual test: REPLACEキャンセル時はCancellationException。エラー通知せずfailureで終了し通知スパムを防ぐ
            android.util.Log.w("CardExtractionWorker", "Work cancelled for deck $deckId", e)
            Result.failure()
        } catch (e: Exception) {
            android.util.Log.e("CardExtractionWorker", "Error during background extraction", e)
            // 一度きりのfailure通知に留め、Result.retry()による無限リトライと複数通知を避ける
            notificationHelper.showErrorNotification(deckId, e.message ?: "Unknown error")
            Result.failure()
        }
    }
}

package com.plath.scancard.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
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

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val deckId = inputData.getLong("deckId", -1)
        val notification = notificationHelper.createForegroundNotification(deckId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                deckId.toInt(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else {
            ForegroundInfo(deckId.toInt(), notification)
        }
    }

    override suspend fun doWork(): Result {
        val deckId = inputData.getLong("deckId", -1)
        val modelId = inputData.getString("modelId") ?: "gemma-4-e2b"
        
        if (deckId == -1L) return Result.failure()

        // MIUI / Android 14+ では 10秒で SystemJobService が onStopJob するため
        // 開始直後に foreground 昇格して kill を防ぐ。64f20a7 以前はフォアグラウンド直接実行で成功していた経緯あり。
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            android.util.Log.w("CardExtractionWorker", "setForeground failed, continue without foreground", e)
        }
        
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

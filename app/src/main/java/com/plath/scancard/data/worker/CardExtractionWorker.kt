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
            // dataSync: shortServiceは~3分のハードリミットがあり実抽出（数分）が途中killされるため変更（Req12.6）
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

        if (deckId == -1L) return Result.failure()

        // MIUI / Android 14+ では 10秒で SystemJobService が onStopJob するため
        // 開始直後に foreground 昇格して kill を防ぐ。
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "setForeground failed, continue without foreground", e)
        }

        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.RUNNING)

        val modelConfig = ModelConfig.AVAILABLE_MODELS.find { it.id == modelId }
            ?: ModelConfig.GEMMA_4_E2B

        return try {
            extractCardsUseCase.extractAndSaveCards(deckId, modelConfig) { current, total ->
                // 進捗ミラー（Req12.14）+ 通知エリア表示（Req12.12）。
                // 進捗通知はアプリ管理の別IDで投稿する（同一IDだとWMが元FGS通知を再投稿して上書きする）
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
            // WorkManagerのstop/cancelセマンティクスを保持するため再スローする（Req12.10）。
            // failure()に変換するとsystem stop後の自動再スケジュールが死に、
            // 「アプリ再起動→job cancel→失敗」の原因になっていた。
            android.util.Log.w(TAG, "Work cancelled for deck $deckId — deferring to WorkManager retry", e)
            throw e
        } catch (e: Exception) {
            // 一時的エラーはバックオフ付きで最大3回リトライし、恒久失敗時のみFAILED+通知（Req12.9/12.11）
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

package com.plath.scancard.domain.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.plath.scancard.data.worker.CardExtractionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundTaskManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun startExtraction(deckId: Long, modelId: String) {
        val data = Data.Builder()
            .putLong("deckId", deckId)
            .putString("modelId", modelId)
            .build()

        val request = OneTimeWorkRequestBuilder<CardExtractionWorker>()
            .setInputData(data)
            .build()

        // Manual test: REPLACEは実行中Workをキャンセルし「Job was cancelled」通知と複数通知スパムを招く。
        // KEEPに変更し、既にRUNNING/ENQUEUEDなら二重起動せず、完了後にのみ再キュー可能にする。
        // ただし「AddMore後すぐ再抽出」したいケースでは前回完了済みならKEEPでも新規実行される。
        // より確実に重複を除去するため、既存WorkがRUNNING/ENQUEUEDならreturnでスキップする明示チェックを追加。
        val existing = workManager.getWorkInfosForUniqueWork("extraction_$deckId").get()
        val isRunning = existing.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        if (isRunning) {
            android.util.Log.w("BackgroundTaskManager", "Extraction already running for deck $deckId — skip duplicate enqueue")
            return
        }
        workManager.enqueueUniqueWork(
            "extraction_$deckId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun getWorkInfo(deckId: Long): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow("extraction_$deckId")
            .map { it.firstOrNull() }
    }

    // IMP-07 7-4 補足（Background/WorkManager通知）: getWorkInfo() の Flow<WorkInfo?> を UI（ExtractionPreviewScreen:41-46）で collect し
    // WorkInfo.State.SUCCEEDED/FAILED 時に NotificationHelper で通知を出す設計。POST_NOTIFICATIONS 権限取得は
    // ExtractionPreviewScreen の permission launcher と整合させること。WorkManagerInitializerは AndroidManifest.xml:45-53 で除去済みか要検証。
}

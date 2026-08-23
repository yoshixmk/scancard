package com.example.scancard.domain.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.scancard.data.worker.CardExtractionWorker
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

        workManager.enqueueUniqueWork(
            "extraction_$deckId",
            ExistingWorkPolicy.REPLACE,
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

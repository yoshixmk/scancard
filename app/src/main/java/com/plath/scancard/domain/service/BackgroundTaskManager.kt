package com.plath.scancard.domain.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.plath.scancard.data.worker.CardExtractionWorker
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.DeckRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundTaskManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository
) {
    private val workManager = WorkManager.getInstance(context)

    fun extractionWorkName(deckId: Long): String = "extraction_$deckId"

    suspend fun startExtraction(deckId: Long, modelId: String) {
        // Set persistent status to PENDING first (Req12.9).
        // This column is the source of truth for resumption judgment upon process death.
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.PENDING)

        if (hasActiveWork(deckId)) {
            android.util.Log.w(TAG, "Extraction already active for deck $deckId — skip duplicate enqueue")
            return
        }
        enqueue(deckId, modelId)
    }

    suspend fun resumeExtraction(deckId: Long) {
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.PENDING)
        enqueue(deckId, ModelConfig.DEFAULT_ID)
    }

    suspend fun hasActiveWork(deckId: Long): Boolean {
        return workManager.getWorkInfosForUniqueWorkFlow(extractionWorkName(deckId))
            .first()
            .any { !it.state.isFinished }
    }

    private suspend fun enqueue(deckId: Long, modelId: String) {
        val data = Data.Builder()
            .putLong(KEY_DECK_ID, deckId)
            .putString(KEY_MODEL_ID, modelId)
            .build()

        val request = OneTimeWorkRequestBuilder<CardExtractionWorker>()
            .setInputData(data)
            // Automatic retry on process death/temporary failure (exponential backoff)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MS, TimeUnit.MILLISECONDS)
            .build()

        // APPEND_OR_REPLACE: Appends to running chains (avoids "Job was cancelled" spam),
        // replaces CANCELLED/FAILED chains so past failures don't silently block retries (Req12.11)
        workManager.enqueueUniqueWork(
            extractionWorkName(deckId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun getWorkInfo(deckId: Long): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(extractionWorkName(deckId))
            .map { it.firstOrNull() }
    }

    companion object {
        private const val TAG = "BackgroundTaskManager"
        const val KEY_DECK_ID = "deckId"
        const val KEY_MODEL_ID = "modelId"
        private const val BACKOFF_DELAY_MS = 10_000L
    }
}

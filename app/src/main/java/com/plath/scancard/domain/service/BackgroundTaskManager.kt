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
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d(TAG, "startExtraction wall=$wall deck=$deckId model=$modelId")
        // Set persistent status to PENDING first (Req12.9).
        // This column is the source of truth for resumption judgment upon process death.
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.PENDING)
        val tPending = android.os.SystemClock.elapsedRealtime() - t0

        val tCheck0 = android.os.SystemClock.elapsedRealtime()
        val active = hasActiveWork(deckId)
        val tCheck = android.os.SystemClock.elapsedRealtime() - tCheck0
        android.util.Log.d(TAG, "hasActiveWork=$active took=${tCheck}ms wall=$wall")
        if (active) {
            android.util.Log.w(TAG, "Extraction already active for deck $deckId — skip duplicate enqueue wall=$wall pending=${tPending}ms check=${tCheck}ms")
            return
        }
        val tEnq0 = android.os.SystemClock.elapsedRealtime()
        enqueue(deckId, modelId)
        val tEnq = android.os.SystemClock.elapsedRealtime() - tEnq0
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d(TAG, "startExtraction enqueued wall=$wall deck=$deckId total=${total}ms pending=${tPending}ms check=${tCheck}ms enqueue=${tEnq}ms")
    }

    suspend fun resumeExtraction(deckId: Long) {
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.PENDING)
        // Use REPLACE for resume: BLOCKED chains (APPEND_OR_REPLACE artifact after kill) would otherwise stay blocked forever.
        enqueue(deckId, ModelConfig.DEFAULT_ID, ExistingWorkPolicy.REPLACE)
    }

    suspend fun hasActiveWork(deckId: Long): Boolean {
        return workManager.getWorkInfosForUniqueWorkFlow(extractionWorkName(deckId))
            .first()
            .any { !it.state.isFinished }
    }

    suspend fun hasRunningOrEnqueuedWork(deckId: Long): Boolean {
        return workManager.getWorkInfosForUniqueWorkFlow(extractionWorkName(deckId))
            .first()
            .any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }

    private suspend fun enqueue(deckId: Long, modelId: String, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE) {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d(TAG, "enqueue wall=$wall deck=$deckId policy=$policy model=$modelId")
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
            policy,
            request
        )
        val dt = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d(TAG, "enqueue done wall=$wall deck=$deckId took=${dt}ms")
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

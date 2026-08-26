package com.plath.scancard.domain.usecase

import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.repository.DeckRepository
import javax.inject.Inject

/**
 * Re-queue extraction for decks left in PENDING/RUNNING status upon app startup (Req12.10).
 * Since WorkManager itself automatically re-executes ENQUEUED work after process death,
 * decks with active work are skipped to prevent double execution.
 */
class ResumePendingExtractionsUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val backgroundTaskManager: BackgroundTaskManager
) {
    suspend operator fun invoke() {
        val stuck = deckRepository.getStuckExtractionDecks()
        if (stuck.isEmpty()) return

        android.util.Log.i(TAG, "Resuming ${stuck.size} stuck extraction deck(s): ${stuck.map { it.id }}")
        for (deck in stuck) {
            if (backgroundTaskManager.hasActiveWork(deck.id)) {
                android.util.Log.d(TAG, "Deck ${deck.id} already has active WorkManager work — skipping")
                continue
            }
            backgroundTaskManager.resumeExtraction(deck.id)
        }
    }

    companion object {
        private const val TAG = "ResumeExtractions"
    }
}

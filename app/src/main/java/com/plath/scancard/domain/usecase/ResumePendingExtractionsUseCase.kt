package com.plath.scancard.domain.usecase

import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.repository.DeckRepository
import javax.inject.Inject

/**
 * アプリ起動時に PENDING/RUNNING のまま取り残されたデッキの抽出を再キューする（Req12.10）。
 * WorkManager 自身もプロセス死亡後の ENQUEUED work を自動再実行するため、
 * アクティブな work が存在するデッキは二重実行防止のためスキップする。
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

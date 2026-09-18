package com.plath.scancard.domain.usecase

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.ml.ExtractedCard
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.repository.ScanRepository
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.TranslationPromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExtractCardsUseCase @Inject constructor(
    private val cardExtractor: GemmaCardExtractor,
    private val scanRepository: ScanRepository,
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val modelRepository: ModelRepository,
    private val promptBuilder: TranslationPromptBuilder,
    private val cardValidator: CardValidator
) {
    /**
     * Processes scans one by one and reports page-level progress to onProgress (Req12.13).
     * onProgress: (current, total) — (0, N) before processing the first page, (i, N) immediately after each page completes.
     */
    suspend fun extractAndSaveCards(
        deckId: Long,
        modelConfig: ModelConfig,
        // Non-empty: process only these scans (add-by-scan must not reprocess older pages).
        // Empty (retry/preview/resume): process all deck scans as before.
        scanIds: List<Long> = emptyList(),
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val deckScans = scanRepository.getScansByDeck(deckId).first()
        val scans = if (scanIds.isEmpty()) deckScans else deckScans.filter { it.id in scanIds }
        val combinedText = scans.joinToString("\n") { it.rawText }

        if (combinedText.isBlank()) {
            // Even if the extraction target is empty, the deck status must be set to completed
            // to avoid infinite resumption loops (Req12.8). No model init or LLM call.
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
            return
        }

        val modelPath = modelRepository.getModelPath(modelConfig)
            ?: throw IllegalStateException("Model ${modelConfig.name} not found. Please download it first.")

        cardExtractor.initialize(modelPath)
        try {
            val total = scans.size
            val extractedPairs = mutableListOf<ExtractedCard>()
            onProgress(0, total)

            for ((index, scan) in scans.withIndex()) {
                // Blank pages (covers, dividers) skip LLM but still advance progress.
                if (scan.rawText.isBlank()) {
                    onProgress(index + 1, total)
                    continue
                }
                val prompt = promptBuilder.buildPrompt(scan.rawText)
                val pagePairs = cardExtractor.extractCards(prompt)
                extractedPairs += pagePairs
                onProgress(index + 1, total)
            }
            
            val cards = extractedPairs.mapNotNull { pair ->
                // Duplicate check
                val existing = cardValidator.findDuplicate(deckId, pair.term)
                if (existing != null && existing.definition == pair.definition) {
                    return@mapNotNull null // Skip exact duplicates
                }
                
                Card(
                    deckId = deckId,
                    term = pair.term,
                    definition = pair.definition,
                    japaneseTranslation = pair.japaneseTranslation
                )
            }
            
            cardRepository.insertCards(cards)
            // Complete card persistence and mark as completed before worker reports success (Req12.8).
            // If this fails, the worker will not return success and status will not become COMPLETED.
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
        } finally {
            cardExtractor.close()
        }
    }
}

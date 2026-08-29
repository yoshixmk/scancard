package com.plath.scancard.domain.usecase

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.repository.ScanRepository
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.PromptValidator
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
    private val promptValidator: PromptValidator,
    private val cardValidator: CardValidator
) {
    /**
     * Processes scans one by one and reports page-level progress to onProgress (Req12.13).
     * onProgress: (current, total) — (0, N) before processing the first page, (i, N) immediately after each page completes.
     */
    suspend fun extractAndSaveCards(
        deckId: Long,
        modelConfig: ModelConfig,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d("ExtractUC", "extractAndSaveCards start wall=$wall deck=$deckId model=${modelConfig.id}")
        val modelPath = modelRepository.getModelPath(modelConfig)
            ?: throw IllegalStateException("Model ${modelConfig.name} not found. Please download it first.")
        android.util.Log.d("ExtractUC", "modelPath=$modelPath wall=$wall")

        val tInit0 = android.os.SystemClock.elapsedRealtime()
        cardExtractor.initialize(modelPath)
        val tInit = android.os.SystemClock.elapsedRealtime() - tInit0
        android.util.Log.d("ExtractUC", "initialize done took=${tInit}ms wall=$wall")

        val scans = scanRepository.getScansByDeck(deckId).first()
        val combinedText = scans.joinToString("\n") { it.rawText }

        if (combinedText.isBlank()) {
            // Even if the extraction target is empty, the deck status must be set to completed
            // to avoid infinite resumption loops (Req12.8).
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
            return
        }

        val total = scans.size
        android.util.Log.d("ExtractUC", "scans loaded size=$total wall=$wall took=${android.os.SystemClock.elapsedRealtime()-t0}ms combinedLen=${combinedText.length}")
        val extractedPairs = mutableListOf<com.plath.scancard.data.ml.ExtractedCard>()
        onProgress(0, total)

        // Manual test: repeat+return@repeat does not break and always runs 3 times, causing
        // memory increase (8.6->9.5GB). Fixed with for+break.
        for ((index, scan) in scans.withIndex()) {
            val tPage0 = android.os.SystemClock.elapsedRealtime()
            val pageText = scan.rawText
            var pagePairs = emptyList<com.plath.scancard.data.ml.ExtractedCard>()
            var currentPrompt = promptBuilder.buildPrompt(pageText)

            for (attempt in 0 until 3) {
                val tAttempt0 = android.os.SystemClock.elapsedRealtime()
                pagePairs = cardExtractor.extractCards(currentPrompt)
                val tAttempt = android.os.SystemClock.elapsedRealtime() - tAttempt0
                android.util.Log.d("ExtractUC", "page ${index+1}/$total attempt $attempt wall=$wall took=${tAttempt}ms pairs=${pagePairs.size} promptLen=${currentPrompt.length}")

                val allValid = pagePairs.all {
                    promptValidator.isValid(it.term, it.definition)
                }

                if (allValid && pagePairs.isNotEmpty()) {
                    break
                }

                if (attempt < 2 && pagePairs.isNotEmpty()) {
                    val failedTerm = pagePairs.find { !promptValidator.isValid(it.term, it.definition) }?.term ?: ""
                    currentPrompt = promptBuilder.buildImprovedPrompt(pageText, failedTerm)
                }
            }

            extractedPairs += pagePairs
            val tPage = android.os.SystemClock.elapsedRealtime() - tPage0
            android.util.Log.d("ExtractUC", "page ${index+1}/$total done wall=$wall took=${tPage}ms accumulated=${extractedPairs.size}")
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
        
        val tPersist0 = android.os.SystemClock.elapsedRealtime()
        cardRepository.insertCards(cards)
        // Complete card persistence and mark as completed before worker reports success (Req12.8).
        // If this fails, the worker will not return success and status will not become COMPLETED.
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
        val tPersist = android.os.SystemClock.elapsedRealtime() - tPersist0
        val totalMs = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("ExtractUC", "extractAndSaveCards done wall=$wall deck=$deckId cards=${cards.size} persist=${tPersist}ms total=${totalMs}ms")
    }
}

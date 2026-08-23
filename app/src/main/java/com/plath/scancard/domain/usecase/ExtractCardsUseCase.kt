package com.plath.scancard.domain.usecase

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
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
    private val modelRepository: ModelRepository,
    private val promptBuilder: TranslationPromptBuilder,
    private val promptValidator: PromptValidator,
    private val cardValidator: CardValidator
) {
    suspend fun extractAndSaveCards(deckId: Long, modelConfig: ModelConfig) {
        val modelPath = modelRepository.getModelPath(modelConfig)
            ?: throw IllegalStateException("Model ${modelConfig.name} not found. Please download it first.")
        
        cardExtractor.initialize(modelPath)

        val scans = scanRepository.getScansByDeck(deckId).first()
        val combinedText = scans.joinToString("\n") { it.rawText }
        
        if (combinedText.isBlank()) return

        var currentPrompt = promptBuilder.buildPrompt(combinedText)
        var extractedPairs = emptyList<com.plath.scancard.data.ml.ExtractedCard>()

        // 手動テスト: repeat+return@repeat はbreakせず3回常に実行されメモリ増(8.6→9.5GB)を招く。for+breakに修正
        for (attempt in 0 until 3) {
            extractedPairs = cardExtractor.extractCards(currentPrompt)
            
            val allValid = extractedPairs.all { 
                promptValidator.isValid(it.term, it.definition) 
            }
            
            if (allValid && extractedPairs.isNotEmpty()) {
                break
            }
            
            if (attempt < 2 && extractedPairs.isNotEmpty()) {
                val failedTerm = extractedPairs.find { !promptValidator.isValid(it.term, it.definition) }?.term ?: ""
                currentPrompt = promptBuilder.buildImprovedPrompt(combinedText, failedTerm)
            }
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
    }
}

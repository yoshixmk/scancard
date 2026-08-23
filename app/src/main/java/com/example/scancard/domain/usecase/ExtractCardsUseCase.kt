package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.data.ml.GemmaCardExtractor
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.repository.CardRepository
import com.example.scancard.domain.repository.ModelRepository
import com.example.scancard.domain.repository.ScanRepository
import com.example.scancard.domain.util.CardValidator
import com.example.scancard.domain.util.PromptValidator
import com.example.scancard.domain.util.TranslationPromptBuilder
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
        var extractedPairs = emptyList<com.example.scancard.data.ml.ExtractedCard>()

        // IMP-07 7-6: design.md 483-543 の3回リトライが配線済み（TranslationPromptBuilder + PromptValidator）
        // 1回目: buildPrompt(combinedText) -> extractCards -> all { promptValidator.isValid } チェック
        // 2-3回目: failedTerm を用いて buildImprovedPrompt(combinedText, failedTerm) で再試行
        // allValid && isNotEmpty で return@repeat（break相当）。無ければ // TODO: retry 3 with PromptValidator コメント。
        // Try up to 3 times if validation fails
        repeat(3) { attempt ->
            extractedPairs = cardExtractor.extractCards(currentPrompt)
            
            val allValid = extractedPairs.all { 
                promptValidator.isValid(it.term, it.definition) 
            }
            
            if (allValid && extractedPairs.isNotEmpty()) {
                return@repeat
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

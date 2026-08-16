package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.data.ml.GemmaCardExtractor
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.repository.CardRepository
import com.example.scancard.domain.repository.ModelRepository
import com.example.scancard.domain.repository.ScanRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExtractCardsUseCase @Inject constructor(
    private val cardExtractor: GemmaCardExtractor,
    private val scanRepository: ScanRepository,
    private val cardRepository: CardRepository,
    private val modelRepository: ModelRepository
) {
    suspend fun extractAndSaveCards(deckId: Long, modelConfig: ModelConfig) {
        val modelPath = modelRepository.getModelPath(modelConfig)
            ?: throw IllegalStateException("Model ${modelConfig.name} not found. Please download it first.")
        
        cardExtractor.initialize(modelPath)

        val scans = scanRepository.getScansByDeck(deckId).first()
        val combinedText = scans.joinToString("\n") { it.rawText }
        
        if (combinedText.isBlank()) return

        val extractedPairs = cardExtractor.extractCards(combinedText)
        
        val cards = extractedPairs.map { pair ->
            Card(
                deckId = deckId,
                term = pair.term,
                definition = pair.definition
            )
        }
        
        cardRepository.insertCards(cards)
    }
}

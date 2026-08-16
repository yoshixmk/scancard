package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.data.ml.GemmaCardExtractor
import com.example.scancard.domain.repository.CardRepository
import com.example.scancard.domain.repository.ScanRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExtractCardsUseCase @Inject constructor(
    private val cardExtractor: GemmaCardExtractor,
    private val scanRepository: ScanRepository,
    private val cardRepository: CardRepository
) {
    suspend fun extractAndSaveCards(deckId: Long) {
        val scans = scanRepository.getScansByDeck(deckId).first()
        val combinedText = scans.joinToString("\n") { it.rawText }
        
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

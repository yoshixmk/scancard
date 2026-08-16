package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend fun exportToTsv(deckId: Long): String {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        return cards.joinToString("\n") { "${it.term}\t${it.definition}" }
    }

    suspend fun exportToCsv(deckId: Long): String {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        return cards.joinToString("\n") { 
            "\"${it.term.replace("\"", "\"\"")}\",\"${it.definition.replace("\"", "\"\"")}\"" 
        }
    }
}

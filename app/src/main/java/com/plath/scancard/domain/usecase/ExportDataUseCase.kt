package com.plath.scancard.domain.usecase

import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.util.ExportManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val exportManager: ExportManager
) {
    suspend fun getExportText(deckId: Long): String {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        return exportManager.exportToTSV(cards)
    }

    suspend fun exportToTsv(deckId: Long): String {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        return exportManager.exportToTSV(cards)
    }

    suspend fun exportToCsv(deckId: Long): String {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        return cards.joinToString("\n") { 
            "\"${it.term.replace("\"", "\"\"")}\",\"${it.definition.replace("\"", "\"\"")}\"" 
        }
    }
}

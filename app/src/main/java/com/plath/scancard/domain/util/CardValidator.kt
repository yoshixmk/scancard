package com.plath.scancard.domain.util

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CardValidator @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend fun findDuplicate(deckId: Long, term: String): Card? {
        val cards = cardRepository.getCardsByDeck(deckId).first()
        val normalizedTerm = term.trim().lowercase()
        return cards.find { it.term.trim().lowercase() == normalizedTerm }
    }
}

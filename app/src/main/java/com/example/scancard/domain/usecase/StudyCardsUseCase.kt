package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StudyCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    fun getCards(deckId: Long): Flow<List<Card>> = cardRepository.getCardsByDeck(deckId)

    suspend fun markAsLearned(cardId: Long) {
        cardRepository.updateLearnedStatus(cardId, true)
    }

    suspend fun markAsReviewNeeded(cardId: Long) {
        cardRepository.updateLearnedStatus(cardId, false)
    }
    
    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
    }
}

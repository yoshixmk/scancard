package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Card
import com.example.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.example.scancard.domain.model.CardStatus

class StudyCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    fun getCards(deckId: Long): Flow<List<Card>> = cardRepository.getCardsByDeck(deckId)

    suspend fun updateStatus(cardId: Long, status: CardStatus) {
        cardRepository.updateCardStatus(cardId, status)
    }

    suspend fun markAsLearned(cardId: Long) {
        cardRepository.updateCardStatus(cardId, CardStatus.LEARNING)
    }

    suspend fun markAsReviewNeeded(cardId: Long) {
        cardRepository.updateCardStatus(cardId, CardStatus.REVIEW)
    }

    suspend fun insertCard(card: Card): Long {
        return cardRepository.insertCard(card)
    }

    suspend fun deleteCard(card: Card) {
        cardRepository.deleteCard(card)
    }
    
    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
    }
}

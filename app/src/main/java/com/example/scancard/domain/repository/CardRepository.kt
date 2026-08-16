package com.example.scancard.domain.repository

import com.example.scancard.data.local.entities.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>
    suspend fun insertCards(cards: List<Card>)
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(card: Card)
    suspend fun updateLearnedStatus(cardId: Long, isLearned: Boolean)
}

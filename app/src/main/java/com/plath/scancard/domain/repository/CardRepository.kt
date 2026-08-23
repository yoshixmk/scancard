package com.plath.scancard.domain.repository

import com.plath.scancard.data.local.entities.Card
import kotlinx.coroutines.flow.Flow

import com.plath.scancard.domain.model.CardStatus

interface CardRepository {
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>
    suspend fun getCardById(id: Long): Card?
    suspend fun insertCards(cards: List<Card>)
    suspend fun insertCard(card: Card): Long
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(card: Card)
    suspend fun updateCardStatus(cardId: Long, status: CardStatus)
}

package com.example.scancard.domain.repository

import com.example.scancard.data.local.entities.Card
import kotlinx.coroutines.flow.Flow

import com.example.scancard.domain.model.CardStatus

interface CardRepository {
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>
    suspend fun getCardById(id: Long): Card?
    suspend fun insertCards(cards: List<Card>)
    suspend fun insertCard(card: Card): Long
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(card: Card)
    suspend fun updateCardStatus(cardId: Long, status: CardStatus)
}

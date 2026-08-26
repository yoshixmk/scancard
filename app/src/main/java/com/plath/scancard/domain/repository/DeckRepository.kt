package com.plath.scancard.domain.repository

import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.domain.model.ExtractionStatus
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun getAllDecks(): Flow<List<Deck>>
    suspend fun getDeckById(id: Long): Deck?
    suspend fun getStuckExtractionDecks(): List<Deck>
    suspend fun updateExtractionStatus(deckId: Long, status: ExtractionStatus)
    suspend fun insertDeck(deck: Deck): Long
    suspend fun updateDeck(deck: Deck)
    suspend fun deleteDeck(deck: Deck)
}

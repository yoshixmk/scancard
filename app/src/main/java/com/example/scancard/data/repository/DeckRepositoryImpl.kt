package com.example.scancard.data.repository

import com.example.scancard.data.local.dao.DeckDao
import com.example.scancard.data.local.entities.Deck
import com.example.scancard.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeckRepositoryImpl @Inject constructor(
    private val deckDao: DeckDao
) : DeckRepository {
    override fun getAllDecks(): Flow<List<Deck>> = deckDao.getAllDecks()
    override suspend fun getDeckById(id: Long): Deck? = deckDao.getDeckById(id)
    override suspend fun insertDeck(deck: Deck): Long = deckDao.insertDeck(deck)
    override suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck)
    override suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)
}

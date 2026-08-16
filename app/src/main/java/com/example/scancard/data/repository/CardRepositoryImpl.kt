package com.example.scancard.data.repository

import com.example.scancard.data.local.dao.CardDao
import com.example.scancard.data.local.entities.Card
import com.example.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {
    override fun getCardsByDeck(deckId: Long): Flow<List<Card>> = cardDao.getCardsByDeck(deckId)
    override suspend fun insertCards(cards: List<Card>) = cardDao.insertCards(cards)
    override suspend fun updateCard(card: Card) = cardDao.updateCard(card)
    override suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)
    override suspend fun updateLearnedStatus(cardId: Long, isLearned: Boolean) = 
        cardDao.updateLearnedStatus(cardId, isLearned)
}

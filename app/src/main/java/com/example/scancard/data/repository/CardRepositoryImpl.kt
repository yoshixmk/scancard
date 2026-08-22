package com.example.scancard.data.repository

import com.example.scancard.data.local.dao.CardDao
import com.example.scancard.data.local.entities.Card
import com.example.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.example.scancard.domain.model.CardStatus

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {
    override fun getCardsByDeck(deckId: Long): Flow<List<Card>> = cardDao.getCardsByDeck(deckId)
    override suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id)
    override suspend fun insertCards(cards: List<Card>) = cardDao.insertCards(cards)
    override suspend fun insertCard(card: Card): Long = cardDao.insertCard(card)
    override suspend fun updateCard(card: Card) = cardDao.updateCard(card)
    override suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)
    override suspend fun updateCardStatus(cardId: Long, status: CardStatus) = 
        cardDao.updateCardStatus(cardId, status)
}

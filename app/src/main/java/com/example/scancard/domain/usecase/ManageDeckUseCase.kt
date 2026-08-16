package com.example.scancard.domain.usecase

import com.example.scancard.data.local.entities.Deck
import com.example.scancard.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository
) {
    fun getDecks(): Flow<List<Deck>> = deckRepository.getAllDecks()

    suspend fun getDeck(id: Long): Deck? = deckRepository.getDeckById(id)

    suspend fun createDeck(title: String): Long {
        if (title.isBlank() || title.length > 100) {
            throw IllegalArgumentException("Title must be between 1 and 100 characters")
        }
        val deck = Deck(title = title)
        return deckRepository.insertDeck(deck)
    }

    suspend fun updateDeck(deck: Deck) {
        if (deck.title.isBlank() || deck.title.length > 100) {
            throw IllegalArgumentException("Title must be between 1 and 100 characters")
        }
        deckRepository.updateDeck(deck.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteDeck(deck: Deck) {
        deckRepository.deleteDeck(deck)
    }
}

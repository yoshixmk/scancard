package com.plath.scancard.domain.util

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeCardRepository(initial: List<Card> = emptyList()) : CardRepository {
    private val flow = MutableStateFlow(initial)
    fun setCards(cards: List<Card>) { flow.value = cards }
    override fun getCardsByDeck(deckId: Long) = flow
    override suspend fun getCardById(id: Long): Card? = flow.value.find { it.id == id }
    override suspend fun insertCards(cards: List<Card>) { flow.value = flow.value + cards }
    override suspend fun insertCard(card: Card): Long { flow.value = flow.value + card; return card.id }
    override suspend fun updateCard(card: Card) { flow.value = flow.value.map { if (it.id == card.id) card else it } }
    override suspend fun deleteCard(card: Card) { flow.value = flow.value.filterNot { it.id == card.id } }
    override suspend fun updateCardStatus(cardId: Long, status: com.plath.scancard.domain.model.CardStatus) {}
}

class CardValidatorTest {

    @Test
    fun findDuplicate_exactMatch_returnsCard() = runTest {
        val card = Card(id = 1, deckId = 1, term = "Hello", definition = "Greeting")
        val repo = FakeCardRepository(listOf(card))
        val validator = CardValidator(repo)
        val result = validator.findDuplicate(1, "Hello")
        assertNotNull(result)
        assertEquals("Hello", result!!.term)
    }

    @Test
    fun findDuplicate_trimNormalization_detectsDuplicate() = runTest {
        val card = Card(id = 1, deckId = 1, term = "  Hello  ", definition = "Greeting")
        val repo = FakeCardRepository(listOf(card))
        val validator = CardValidator(repo)
        // Searching with extra spaces should match trimmed term
        assertNotNull(validator.findDuplicate(1, "Hello"))
        assertNotNull(validator.findDuplicate(1, "  Hello"))
        assertNotNull(validator.findDuplicate(1, "Hello  "))
    }

    @Test
    fun findDuplicate_lowercaseNormalization_detectsDuplicate() = runTest {
        val card = Card(id = 1, deckId = 1, term = "Hello", definition = "Greeting")
        val repo = FakeCardRepository(listOf(card))
        val validator = CardValidator(repo)
        assertNotNull(validator.findDuplicate(1, "hello"))
        assertNotNull(validator.findDuplicate(1, "HELLO"))
        assertNotNull(validator.findDuplicate(1, "HeLLo"))
    }

    @Test
    fun findDuplicate_combinedTrimLowercase_detectsDuplicate() = runTest {
        val card = Card(id = 1, deckId = 1, term = "  Hello World  ", definition = "Greeting")
        val repo = FakeCardRepository(listOf(card))
        val validator = CardValidator(repo)
        assertNotNull(validator.findDuplicate(1, "hello world"))
        assertNotNull(validator.findDuplicate(1, "  HELLO WORLD  "))
        assertNotNull(validator.findDuplicate(1, "Hello World"))
    }

    @Test
    fun findDuplicate_noMatch_returnsNull() = runTest {
        val card = Card(id = 1, deckId = 1, term = "Hello", definition = "Greeting")
        val repo = FakeCardRepository(listOf(card))
        val validator = CardValidator(repo)
        assertNull(validator.findDuplicate(1, "World"))
        assertNull(validator.findDuplicate(1, "Helloo"))
    }

    @Test
    fun findDuplicate_emptyRepo_returnsNull() = runTest {
        val repo = FakeCardRepository(emptyList())
        val validator = CardValidator(repo)
        assertNull(validator.findDuplicate(1, "Hello"))
    }

    @Test
    fun findDuplicate_multipleCards_findsCorrect() = runTest {
        val cards = listOf(
            Card(id = 1, deckId = 1, term = "Apple", definition = "Fruit"),
            Card(id = 2, deckId = 1, term = "Banana", definition = "Fruit"),
            Card(id = 3, deckId = 1, term = "Cherry", definition = "Fruit")
        )
        val repo = FakeCardRepository(cards)
        val validator = CardValidator(repo)
        assertNotNull(validator.findDuplicate(1, "banana"))
        assertEquals(2L, validator.findDuplicate(1, "  BANANA ")!!.id)
    }
}

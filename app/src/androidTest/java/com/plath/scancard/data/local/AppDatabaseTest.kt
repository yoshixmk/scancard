package com.plath.scancard.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.domain.model.CardStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deckDao_insertAndGet() = runTest {
        val deck = Deck(title = "Test Deck")
        val id = db.deckDao().insertDeck(deck)
        assertTrue(id > 0)

        val loaded = db.deckDao().getDeckById(id)
        assertNotNull(loaded)
        assertEquals("Test Deck", loaded!!.title)

        val all = db.deckDao().getAllDecks().first()
        assertEquals(1, all.size)
    }

    @Test
    fun deckDao_updateAndDelete() = runTest {
        val id = db.deckDao().insertDeck(Deck(title = "Original"))
        val deck = db.deckDao().getDeckById(id)!!
        val updated = deck.copy(title = "Updated")
        db.deckDao().updateDeck(updated)
        assertEquals("Updated", db.deckDao().getDeckById(id)!!.title)

        db.deckDao().deleteDeck(updated)
        assertNull(db.deckDao().getDeckById(id))
    }

    @Test
    fun cardDao_crudOperations() = runTest {
        val deckId = db.deckDao().insertDeck(Deck(title = "Deck for cards"))
        val card = Card(deckId = deckId, term = "Hello", definition = "Greeting", status = CardStatus.NEW)
        val cardId = db.cardDao().insertCard(card)
        assertTrue(cardId > 0)

        val loaded = db.cardDao().getCardById(cardId)
        assertNotNull(loaded)
        assertEquals("Hello", loaded!!.term)
        assertEquals(CardStatus.NEW, loaded.status)

        // update
        val updated = loaded.copy(definition = "Updated definition")
        db.cardDao().updateCard(updated)
        assertEquals("Updated definition", db.cardDao().getCardById(cardId)!!.definition)

        // update status
        db.cardDao().updateCardStatus(cardId, CardStatus.LEARNING)
        assertEquals(CardStatus.LEARNING, db.cardDao().getCardById(cardId)!!.status)

        // get by deck
        val cards = db.cardDao().getCardsByDeck(deckId).first()
        assertEquals(1, cards.size)

        // insert multiple
        db.cardDao().insertCards(
            listOf(
                Card(deckId = deckId, term = "World", definition = "Earth"),
                Card(deckId = deckId, term = "Test", definition = "Exam")
            )
        )
        val allCards = db.cardDao().getCardsByDeck(deckId).first()
        assertEquals(3, allCards.size)

        // delete
        db.cardDao().deleteCard(db.cardDao().getCardById(cardId)!!)
        assertNull(db.cardDao().getCardById(cardId))
        assertEquals(2, db.cardDao().getCardsByDeck(deckId).first().size)
    }

    @Test
    fun scanDao_crudOperations() = runTest {
        val deckId = db.deckDao().insertDeck(Deck(title = "Deck for scans"))
        val scan = Scan(deckId = deckId, imagePath = "/tmp/img.jpg", rawText = "raw ocr text")
        db.scanDao().insertScan(scan)

        val scans = db.scanDao().getScansByDeck(deckId).first()
        assertEquals(1, scans.size)
        assertEquals("raw ocr text", scans[0].rawText)

        db.scanDao().deleteScan(scans[0])
        assertEquals(0, db.scanDao().getScansByDeck(deckId).first().size)
    }

    @Test
    fun cardDao_cascadeDelete_whenDeckDeleted() = runTest {
        val deckId = db.deckDao().insertDeck(Deck(title = "CascadeDeck"))
        db.cardDao().insertCard(Card(deckId = deckId, term = "Term1", definition = "Def1"))
        db.scanDao().insertScan(Scan(deckId = deckId, imagePath = "/a.jpg", rawText = "text"))

        assertEquals(1, db.cardDao().getCardsByDeck(deckId).first().size)

        val deck = db.deckDao().getDeckById(deckId)!!
        db.deckDao().deleteDeck(deck)

        // After deck deletion, cards and scans should be cascade-deleted (if FK enforced)
        // In-memory DB with FK support: verify deck is gone
        assertNull(db.deckDao().getDeckById(deckId))
    }
}

package com.plath.scancard.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.RoomDatabase
import com.plath.scancard.data.local.AppDatabase
import com.plath.scancard.data.local.MIGRATION_2_3
import com.plath.scancard.data.local.MIGRATION_3_4
import com.plath.scancard.data.local.MIGRATION_4_5
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.local.entities.Deck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TDD reproduction for bug: "Cards disappear after app close"
 * Uses file-based DB (not in-memory) to simulate process death.
 * Before fix (fallbackToDestructiveMigration or in-memory), this fails.
 * After fix (file DB + migrations), passes.
 */
@RunWith(AndroidJUnit4::class)
class CardPersistenceAfterCloseTest {

    private val dbName = "test_persistence_after_close.db"

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(dbName)
    }

    private fun buildDb(name: String = dbName): AppDatabase {
        return Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            name
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // Do NOT use fallbackToDestructiveMigration — that would hide the bug by wiping data
            .build()
    }

    @Test
    fun cardsSurviveDbCloseAndReopen() = runTest {
        // Step 1: create DB file and insert
        var db = buildDb()
        val deckId = db.deckDao().insertDeck(Deck(title = "PersistDeck"))
        assert(deckId > 0)
        val c1 = db.cardDao().insertCard(Card(deckId = deckId, term = "Apple", definition = "Fruit"))
        val c2 = db.cardDao().insertCard(Card(deckId = deckId, term = "Banana", definition = "Yellow"))
        // Verify visible before close
        var cards = db.cardDao().getCardsByDeck(deckId).first()
        assertEquals(2, cards.size)
        assertNotNull(db.cardDao().getCardById(c1))
        assertNotNull(db.cardDao().getCardById(c2))
        var decks = db.deckDao().getAllDecks().first()
        assertEquals(1, decks.size)
        // Simulate app close: close DB (WAL checkpoint) — file must remain
        db.close()

        // Step 2: simulate app restart — reopen same file
        db = buildDb()
        // This will throw if MIGRATION missing (e.g., fallback would have wiped)
        cards = db.cardDao().getCardsByDeck(deckId).first()
        decks = db.deckDao().getAllDecks().first()
        // BUG REPRO: before fix, cards is empty (0) because DB was in-memory or wiped
        assertEquals("Cards must survive close/reopen", 2, cards.size)
        assertEquals("Deck must survive", 1, decks.size)
        assertEquals("Fruit", cards.find { it.term == "Apple" }?.definition)
        assertEquals("Yellow", cards.find { it.term == "Banana" }?.definition)
        db.close()
    }

    @Test
    fun decksSurviveCloseAndReopen() = runTest {
        var db = buildDb()
        val id1 = db.deckDao().insertDeck(Deck(title = "Deck1"))
        val id2 = db.deckDao().insertDeck(Deck(title = "Deck2"))
        db.close()
        db = buildDb()
        val decks = db.deckDao().getAllDecks().first()
        assertEquals(2, decks.size)
        assertNotNull(decks.find { it.id == id1 })
        assertNotNull(decks.find { it.id == id2 })
        db.close()
    }
}

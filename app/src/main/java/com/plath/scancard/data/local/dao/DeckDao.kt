package com.plath.scancard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.domain.model.ExtractionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY updatedAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: Long): Deck?

    @Query("SELECT * FROM decks WHERE extractionStatus IN ('PENDING', 'RUNNING')")
    suspend fun getStuckExtractionDecks(): List<Deck>

    @Query("UPDATE decks SET extractionStatus = :status, updatedAt = :updatedAt WHERE id = :deckId")
    suspend fun updateExtractionStatus(
        deckId: Long,
        status: ExtractionStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)
}

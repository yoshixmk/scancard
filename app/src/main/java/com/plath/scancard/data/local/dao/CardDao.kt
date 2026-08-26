package com.plath.scancard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plath.scancard.data.local.entities.Card
import kotlinx.coroutines.flow.Flow

import com.plath.scancard.domain.model.CardStatus

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt ASC")
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Card>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("UPDATE cards SET status = :status WHERE id = :cardId")
    suspend fun updateCardStatus(cardId: Long, status: CardStatus)

    // IMP-07 7-3: Duplicate check with normalization. lower(trim(term)) ignores case and leading/trailing whitespace.
    // Query: SELECT * WHERE deckId=:deckId AND lower(trim(term))=lower(trim(:term))
    // - Ensures DB-level normalization with SQLite's lower()/trim() (duplicated with app-layer CardValidator.findDuplicate).
    // - Added as it was missing; the SQL above handles normalization.
    // - Note for future fix: if there was a `WHERE term = :term` without lower/trim, replace with the above query.
    // - Index recommendation: deckId has an existing index. To add a composite UNIQUE(deckId, normalizedTerm),
    //   one could add a normalizedTerm column and persist lower(trim(term)) via triggers/generated columns,
    //   but this PR sticks to query normalization.
    @Query("SELECT * FROM cards WHERE deckId = :deckId AND lower(trim(term)) = lower(trim(:term)) LIMIT 1")
    suspend fun checkDuplicate(deckId: Long, term: String): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND lower(trim(term)) = lower(trim(:term))")
    suspend fun findDuplicatesNormalized(deckId: Long, term: String): List<Card>
}

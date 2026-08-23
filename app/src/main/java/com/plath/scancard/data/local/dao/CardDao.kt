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

    // IMP-07 7-3: 正規化対応の重複チェック。lower(trim(term)) で大文字小文字・前後空白を無視。
    // クエリ: SELECT * WHERE deckId=:deckId AND lower(trim(term))=lower(trim(:term))
    // - SQLiteの lower()/trim() でDBレベル正規化を担保（アプリ層 CardValidator.findDuplicate と二重化）。
    // - 既存 checkDuplicate が無かったため新規追加。上記SQLは正規化対応済み。
    // - 修正案メモ: もし lower/trim なしの `WHERE term = :term` だった場合は上記クエリへ置換すること。
    // - Index推奨: deckId は既存 Index あり。複合 UNIQUE(deckId, normalizedTerm) を張るなら normalizedTerm 列を追加して
    //   trigger/生成列で lower(trim(term)) を永続化し UNIQUE 制約を付与する方法もあるが、本PRではクエリ正規化に留める。
    @Query("SELECT * FROM cards WHERE deckId = :deckId AND lower(trim(term)) = lower(trim(:term)) LIMIT 1")
    suspend fun checkDuplicate(deckId: Long, term: String): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND lower(trim(term)) = lower(trim(:term))")
    suspend fun findDuplicatesNormalized(deckId: Long, term: String): List<Card>
}

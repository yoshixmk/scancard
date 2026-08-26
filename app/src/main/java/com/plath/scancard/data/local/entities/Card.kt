package com.plath.scancard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.plath.scancard.domain.model.CardStatus

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deckId"])]
)
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val term: String,
    val definition: String,
    val japaneseTranslation: String = "",
    val status: CardStatus = CardStatus.NEW,
    val createdAt: Long = System.currentTimeMillis(),
    // IMP-07 7-1: Duplicate detection columns (design.md 272-284).
    // With default values to avoid breaking existing DB loads and constructors.
    @ColumnInfo(name = "isDuplicate")
    val isDuplicate: Boolean = false,
    @ColumnInfo(name = "duplicateOfId")
    val duplicateOfId: Long? = null
)

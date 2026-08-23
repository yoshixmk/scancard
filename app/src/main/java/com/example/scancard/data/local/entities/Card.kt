package com.example.scancard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.example.scancard.domain.model.CardStatus

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
    // IMP-07 7-1: 重複検出列（design.md 272-284）。デフォルト値付きで既存DB読込・既存コンストラクタを壊さない
    @ColumnInfo(name = "isDuplicate")
    val isDuplicate: Boolean = false,
    @ColumnInfo(name = "duplicateOfId")
    val duplicateOfId: Long? = null
)

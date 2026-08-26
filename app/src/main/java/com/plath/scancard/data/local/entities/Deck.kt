package com.plath.scancard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.plath.scancard.domain.model.ExtractionStatus

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val extractionStatus: ExtractionStatus = ExtractionStatus.NONE
)

package com.example.scancard.data.local

import androidx.room.TypeConverter
import com.example.scancard.domain.model.CardStatus

class Converters {
    @TypeConverter
    fun fromCardStatus(value: CardStatus): String {
        return value.name
    }

    @TypeConverter
    fun toCardStatus(value: String): CardStatus {
        return CardStatus.valueOf(value)
    }
}

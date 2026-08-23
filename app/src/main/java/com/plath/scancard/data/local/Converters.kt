package com.plath.scancard.data.local

import androidx.room.TypeConverter
import com.plath.scancard.domain.model.CardStatus

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

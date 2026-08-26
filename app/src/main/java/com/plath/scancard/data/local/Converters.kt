package com.plath.scancard.data.local

import androidx.room.TypeConverter
import com.plath.scancard.domain.model.CardStatus
import com.plath.scancard.domain.model.ExtractionStatus

class Converters {
    @TypeConverter
    fun fromCardStatus(value: CardStatus): String {
        return value.name
    }

    @TypeConverter
    fun toCardStatus(value: String): CardStatus {
        return CardStatus.valueOf(value)
    }

    @TypeConverter
    fun fromExtractionStatus(value: ExtractionStatus): String {
        return value.name
    }

    @TypeConverter
    fun toExtractionStatus(value: String): ExtractionStatus {
        return ExtractionStatus.valueOf(value)
    }
}

package com.example.scancard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.scancard.data.local.dao.CardDao
import com.example.scancard.data.local.dao.DeckDao
import com.example.scancard.data.local.dao.ScanDao
import com.example.scancard.data.local.entities.Card
import com.example.scancard.data.local.entities.Deck
import com.example.scancard.data.local.entities.Scan

@Database(
    entities = [Deck::class, Card::class, Scan::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun scanDao(): ScanDao
}

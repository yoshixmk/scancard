package com.plath.scancard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.plath.scancard.data.local.dao.CardDao
import com.plath.scancard.data.local.dao.DeckDao
import com.plath.scancard.data.local.dao.ScanDao
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.data.local.entities.Scan

import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cards ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cards ADD COLUMN duplicateOfId INTEGER")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE decks ADD COLUMN extractionStatus TEXT NOT NULL DEFAULT 'NONE'")
    }
}

@Database(
    entities = [Deck::class, Card::class, Scan::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun scanDao(): ScanDao
}

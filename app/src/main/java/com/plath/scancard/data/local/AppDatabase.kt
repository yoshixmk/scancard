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
        try { db.execSQL("ALTER TABLE cards ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE cards ADD COLUMN duplicateOfId INTEGER") } catch (_: Exception) {}
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE decks ADD COLUMN extractionStatus TEXT NOT NULL DEFAULT 'NONE'") } catch (_: Exception) {}
    }
}

// 4->5: repair for DBs created at v4 with missing column (exportSchema false era)
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE decks ADD COLUMN extractionStatus TEXT NOT NULL DEFAULT 'NONE'") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE cards ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE cards ADD COLUMN duplicateOfId INTEGER") } catch (_: Exception) {}
    }
}

@Database(
    entities = [Deck::class, Card::class, Scan::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun scanDao(): ScanDao
}

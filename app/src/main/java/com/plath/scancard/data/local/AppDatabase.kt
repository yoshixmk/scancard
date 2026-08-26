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

// IMP-07 7-2: Added Card.isDuplicate, Card.duplicateOfId columns in version 2->3
// Added Deck.extractionStatus column in version 3->4 (Req12.8-12.11 Persistence and resumption of extraction results).
// Use fallbackToDestructiveMigration() with exportSchema=false and no AutoMigration during build (for development).
// Generate schema 2.json for AutoMigration and enable the following before production release:
//   exportSchema=true + autoMigrations=[AutoMigration(from=2,to=3)] + room { schemaDirectory("$projectDir/schemas") }
// Use the following if manual Migration is required:
// val MIGRATION_2_3 = object : Migration(2, 3) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         db.execSQL("ALTER TABLE cards ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0")
//         db.execSQL("ALTER TABLE cards ADD COLUMN duplicateOfId INTEGER")
//     }
// }
// DatabaseModule.kt's fallbackToDestructiveMigration() is for development. Remove and register MIGRATION_2_3 with addMigrations() in production.
@Database(
    entities = [Deck::class, Card::class, Scan::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun scanDao(): ScanDao
}

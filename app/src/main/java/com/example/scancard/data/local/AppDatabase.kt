package com.example.scancard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.scancard.data.local.dao.CardDao
import com.example.scancard.data.local.dao.DeckDao
import com.example.scancard.data.local.dao.ScanDao
import com.example.scancard.data.local.entities.Card
import com.example.scancard.data.local.entities.Deck
import com.example.scancard.data.local.entities.Scan

import androidx.room.TypeConverters

// IMP-07 7-2: version 2->3 で Card.isDuplicate, Card.duplicateOfId 列追加
// Build時は exportSchema=false + AutoMigrationなしで fallbackToDestructiveMigration() に委譲（開発用）。
// 本番リリース前に AutoMigration 用の schema 2.json を生成し、下記を有効化すること:
//   exportSchema=true + autoMigrations=[AutoMigration(from=2,to=3)] + room { schemaDirectory("$projectDir/schemas") }
// 手動Migrationが必要な場合は以下を使用:
// val MIGRATION_2_3 = object : Migration(2, 3) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         db.execSQL("ALTER TABLE cards ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0")
//         db.execSQL("ALTER TABLE cards ADD COLUMN duplicateOfId INTEGER")
//     }
// }
// DatabaseModule.kt の fallbackToDestructiveMigration() は開発用。本番では除去し MIGRATION_2_3 を addMigrations() で登録。
@Database(
    entities = [Deck::class, Card::class, Scan::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun scanDao(): ScanDao
}

package com.plath.scancard.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.plath.scancard.data.local.AppDatabase
import com.plath.scancard.data.local.MIGRATION_2_3
import com.plath.scancard.data.local.MIGRATION_3_4
import com.plath.scancard.data.local.MIGRATION_4_5
import com.plath.scancard.data.local.dao.CardDao
import com.plath.scancard.data.local.dao.DeckDao
import com.plath.scancard.data.local.dao.ScanDao
import com.plath.scancard.data.ml.CardResponseParser
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.ExportManager
import com.plath.scancard.domain.util.PromptValidator
import com.plath.scancard.domain.util.TranslationPromptBuilder
import com.plath.scancard.util.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper = 
        NotificationHelper(context)

    @Provides
    @Singleton
    fun provideExportManager(): ExportManager = ExportManager()

    @Provides
    @Singleton
    fun provideTranslationPromptBuilder(): TranslationPromptBuilder = TranslationPromptBuilder()

    @Provides
    @Singleton
    fun providePromptValidator(): PromptValidator = PromptValidator()
    
    @Provides
    @Singleton
    fun provideCardValidator(cardRepository: com.plath.scancard.domain.repository.CardRepository): CardValidator = 
        CardValidator(cardRepository)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "scancard_db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    @Provides
    fun provideDeckDao(database: AppDatabase): DeckDao = database.deckDao()

    @Provides
    fun provideCardDao(database: AppDatabase): CardDao = database.cardDao()

    @Provides
    fun provideScanDao(database: AppDatabase): ScanDao = database.scanDao()

    @Provides
    @Singleton
    fun provideCardResponseParser(): CardResponseParser = CardResponseParser()
}

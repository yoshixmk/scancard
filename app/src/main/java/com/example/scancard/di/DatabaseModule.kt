package com.example.scancard.di

import android.content.Context
import androidx.room.Room
import com.example.scancard.data.local.AppDatabase
import com.example.scancard.data.local.dao.CardDao
import com.example.scancard.data.local.dao.DeckDao
import com.example.scancard.data.local.dao.ScanDao
import com.example.scancard.data.ml.CardResponseParser
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "scancard_db"
        ).build()
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

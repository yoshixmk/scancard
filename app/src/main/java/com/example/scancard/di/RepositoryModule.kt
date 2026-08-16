package com.example.scancard.di

import com.example.scancard.data.repository.CardRepositoryImpl
import com.example.scancard.data.repository.DeckRepositoryImpl
import com.example.scancard.data.repository.ModelRepositoryImpl
import com.example.scancard.data.repository.ScanRepositoryImpl
import com.example.scancard.domain.repository.CardRepository
import com.example.scancard.domain.repository.DeckRepository
import com.example.scancard.domain.repository.ModelRepository
import com.example.scancard.domain.repository.ScanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeckRepository(
        deckRepositoryImpl: DeckRepositoryImpl
    ): DeckRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        cardRepositoryImpl: CardRepositoryImpl
    ): CardRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        scanRepositoryImpl: ScanRepositoryImpl
    ): ScanRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(
        modelRepositoryImpl: ModelRepositoryImpl
    ): ModelRepository
}

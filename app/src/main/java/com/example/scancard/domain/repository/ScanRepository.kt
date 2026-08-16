package com.example.scancard.domain.repository

import com.example.scancard.data.local.entities.Scan
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun getScansByDeck(deckId: Long): Flow<List<Scan>>
    suspend fun insertScan(scan: Scan)
    suspend fun deleteScan(scan: Scan)
}

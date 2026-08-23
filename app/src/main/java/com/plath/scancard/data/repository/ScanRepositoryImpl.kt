package com.plath.scancard.data.repository

import com.plath.scancard.data.local.dao.ScanDao
import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val scanDao: ScanDao
) : ScanRepository {
    override fun getScansByDeck(deckId: Long): Flow<List<Scan>> = scanDao.getScansByDeck(deckId)
    override suspend fun insertScan(scan: Scan) = scanDao.insertScan(scan)
    override suspend fun deleteScan(scan: Scan) = scanDao.deleteScan(scan)
}

package com.example.scancard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.scancard.data.local.entities.Scan
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans WHERE deckId = :deckId")
    fun getScansByDeck(deckId: Long): Flow<List<Scan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: Scan)

    @Delete
    suspend fun deleteScan(scan: Scan)
}

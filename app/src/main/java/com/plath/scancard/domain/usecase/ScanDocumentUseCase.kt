package com.plath.scancard.domain.usecase

import android.net.Uri
import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.data.ml.TextRecognitionManager
import com.plath.scancard.domain.repository.ScanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ScanDocumentUseCase @Inject constructor(
    private val textRecognitionManager: TextRecognitionManager,
    private val scanRepository: ScanRepository
) {
    // E2E debug helper: insert dummy scan without GMS/MLKit, bypass OCR
    suspend fun insertDummyScan(deckId: Long): Long {
        val dummyText = "Apple: A fruit\nBanana: Yellow fruit\nCat: Animal that meows"
        val scan = Scan(
            deckId = deckId,
            imagePath = "dummy://e2e",
            rawText = dummyText
        )
        scanRepository.insertScan(scan)
        return deckId
    }

    // Run OCR in parallel as pages are independent (Req18.1). ML Kit's recognizer is concurrency-safe.
    // Results and DB insertion maintain page order.
    suspend fun processScannedPages(deckId: Long, pageUris: List<Uri>): List<Scan> = coroutineScope {
        val scans: List<Scan> = pageUris.mapIndexed { _, uri ->
            async {
                val text = textRecognitionManager.recognizeText(uri)
                Scan(
                    deckId = deckId,
                    imagePath = uri.toString(),
                    rawText = text
                )
            }
        }.awaitAll()
        scans.forEach { scanRepository.insertScan(it) }
        scans
    }
}

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
        android.util.Log.d("ScanDocumentUseCase", "Inserted dummy scan for deck $deckId")
        return deckId
    }

    // Run OCR in parallel as pages are independent (Req18.1). ML Kit's recognizer is concurrency-safe.
    // Results and DB insertion maintain page order.
    suspend fun processScannedPages(deckId: Long, pageUris: List<Uri>): List<Scan> = coroutineScope {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d("ScanDocumentUC", "processScannedPages start wall=$wall deck=$deckId pages=${pageUris.size}")
        val scans: List<Scan> = pageUris.mapIndexed { idx, uri ->
            async {
                val tPage0 = android.os.SystemClock.elapsedRealtime()
                val text = textRecognitionManager.recognizeText(uri)
                val tPage = android.os.SystemClock.elapsedRealtime() - tPage0
                android.util.Log.d("ScanDocumentUC", "page ${idx+1}/${pageUris.size} ocr wall=$wall took=${tPage}ms len=${text.length} uri=$uri")
                Scan(
                    deckId = deckId,
                    imagePath = uri.toString(),
                    rawText = text
                )
            }
        }.awaitAll()
        val tOcrTotal = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("ScanDocumentUC", "ocr all pages done wall=$wall took=${tOcrTotal}ms")
        val tInsert0 = android.os.SystemClock.elapsedRealtime()
        scans.forEach { scanRepository.insertScan(it) }
        val tInsert = android.os.SystemClock.elapsedRealtime() - tInsert0
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("ScanDocumentUC", "processScannedPages done wall=$wall deck=$deckId total=${total}ms ocr=${tOcrTotal}ms insert=${tInsert}ms")
        scans
    }
}

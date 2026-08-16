package com.example.scancard.domain.usecase

import android.net.Uri
import com.example.scancard.data.local.entities.Scan
import com.example.scancard.data.ml.TextRecognitionManager
import com.example.scancard.domain.repository.ScanRepository
import javax.inject.Inject

class ScanDocumentUseCase @Inject constructor(
    private val textRecognitionManager: TextRecognitionManager,
    private val scanRepository: ScanRepository
) {
    suspend fun processScannedPages(deckId: Long, pageUris: List<Uri>): List<Scan> {
        val scans = mutableListOf<Scan>()
        for (uri in pageUris) {
            val recognizedText = textRecognitionManager.recognizeText(uri)
            val scan = Scan(
                deckId = deckId,
                imagePath = uri.toString(),
                rawText = recognizedText
            )
            scanRepository.insertScan(scan)
            scans.add(scan)
        }
        return scans
    }
}

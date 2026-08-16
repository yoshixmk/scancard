package com.example.scancard.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scancard.domain.usecase.ManageDeckUseCase
import com.example.scancard.domain.usecase.ScanDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanDocumentUseCase: ScanDocumentUseCase,
    private val manageDeckUseCase: ManageDeckUseCase
) : ViewModel() {

    private val _scannedPages = MutableStateFlow<List<Uri>>(emptyList())
    val scannedPages = _scannedPages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun addPages(uris: List<Uri>) {
        _scannedPages.value = _scannedPages.value + uris
    }

    fun removePage(uri: Uri) {
        _scannedPages.value = _scannedPages.value - uri
    }

    fun processScans(deckId: Long, onComplete: (Long) -> Unit) {
        android.util.Log.d("ScanVM", "Processing scans for deckId input: $deckId")
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val targetDeckId = if (deckId <= 0) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val newId = manageDeckUseCase.createDeck("Scan $dateStr")
                    android.util.Log.d("ScanVM", "Created new deck with ID: $newId")
                    newId
                } else {
                    deckId
                }
                scanDocumentUseCase.processScannedPages(targetDeckId, _scannedPages.value)
                android.util.Log.d("ScanVM", "Scan processing complete. Navigating to extraction with ID: $targetDeckId")
                onComplete(targetDeckId)
            } catch (e: Exception) {
                android.util.Log.e("ScanVM", "Error processing scans", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

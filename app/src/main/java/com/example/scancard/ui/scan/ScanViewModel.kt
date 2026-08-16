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
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val targetDeckId = if (deckId <= 0) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    manageDeckUseCase.createDeck("Scan $dateStr")
                } else {
                    deckId
                }
                scanDocumentUseCase.processScannedPages(targetDeckId, _scannedPages.value)
                onComplete(targetDeckId)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

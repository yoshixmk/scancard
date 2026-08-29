package com.plath.scancard.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.plath.scancard.data.ml.TextRecognitionManager
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.usecase.ManageDeckUseCase
import com.plath.scancard.domain.usecase.OcrPipelineState
import com.plath.scancard.domain.usecase.OcrWordPipelineUseCase
import com.plath.scancard.domain.usecase.ScanDocumentUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
    private val scanDocumentUseCase: ScanDocumentUseCase,
    private val manageDeckUseCase: ManageDeckUseCase,
    private val modelRepository: ModelRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val ocrWordPipeline: OcrWordPipelineUseCase
) : ViewModel() {

    private val _scannedPages = MutableStateFlow<List<Uri>>(emptyList())
    val scannedPages = _scannedPages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    val ocrPipelineState = ocrWordPipeline.state

    // DEBUG E2E helper: ML Kit pre-OCR verification on bundled sample.jpg (Req4)
    private val _ocrSampleText = MutableStateFlow<String?>(null)
    val ocrSampleText = _ocrSampleText.asStateFlow()
    private val _ocrSampleBlocks = MutableStateFlow<String?>(null)
    val ocrSampleBlocks = _ocrSampleBlocks.asStateFlow()
    fun runMlkitOcrSample() {
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            try {
                val bitmap = context.assets.open("sample.jpg").use { android.graphics.BitmapFactory.decodeStream(it) }
                if (bitmap == null) {
                    _ocrSampleText.value = "ERROR: decode failed"
                    return@launch
                }
                val textRecognitionManager = TextRecognitionManager(context)
                val t1 = android.os.SystemClock.elapsedRealtime()
                val text = textRecognitionManager.recognizeTextFromBitmap(bitmap)
                val t2 = android.os.SystemClock.elapsedRealtime()
                _ocrSampleText.value = text
                _ocrSampleBlocks.value = "blocks:${text.length}"
                android.util.Log.d("ScanVM", "MlkitOcrSample: decode=${t1-t0}ms ocr=${t2-t1}ms total=${t2-t0}ms len=${text.length} text=${text.take(200)}")
            } catch (e: Exception) {
                val t2 = android.os.SystemClock.elapsedRealtime()
                _ocrSampleText.value = "ERROR: ${e.message}"
                android.util.Log.e("ScanVM", "MlkitOcrSample failed after ${t2-t0}ms", e)
            }
        }
    }

    // Foreground pipeline (Req3): OCR → WordGen → Persistence without WorkManager
    fun processScansWithPipeline(deckId: Long, onComplete: (Long, Boolean) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val targetDeckId = if (deckId <= 0) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    manageDeckUseCase.createDeck("Scan $dateStr")
                } else deckId
                val cards = ocrWordPipeline.execute(targetDeckId, _scannedPages.value)
                onComplete(targetDeckId, cards.isNotEmpty())
            } catch (e: Exception) {
                android.util.Log.e("ScanVM", "Pipeline failed", e)
            } finally { _isProcessing.value = false }
        }
    }

    fun addPages(uris: List<Uri>) {
        _scannedPages.value = _scannedPages.value + uris
    }

    fun removePage(uri: Uri) {
        _scannedPages.value = _scannedPages.value - uri
    }

    /**
     * Fast Mode (Req18.2/18.3) after OCR: If model is ready, skip extraction preview screen
     * and start background extraction directly then navigate to deck detail (2nd arg of
     * onComplete = true). If model is not ready, navigate to ExtractionPreviewScreen
     * as usual (2nd arg = false, Req18.4).
     */
    fun processScans(deckId: Long, onComplete: (Long, Boolean) -> Unit) {
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
                android.util.Log.d("ScanVM", "Scan processing complete. Navigating with ID: $targetDeckId")

                modelRepository.checkModelStatus(ModelConfig.GEMMA_4_E2B)
                val modelReady = modelRepository.modelState.value is ModelState.Ready
                if (modelReady) {
                    android.util.Log.d("ScanVM", "Fast mode: model ready — starting extraction directly")
                    backgroundTaskManager.startExtraction(targetDeckId, ModelConfig.DEFAULT_ID)
                }
                onComplete(targetDeckId, modelReady)
            } catch (e: Exception) {
                android.util.Log.e("ScanVM", "Error processing scans", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun insertDummyScanForE2E(deckId: Long, onComplete: (Long, Boolean) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val targetDeckId = if (deckId <= 0) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    manageDeckUseCase.createDeck("Scan $dateStr")
                } else deckId
                scanDocumentUseCase.insertDummyScan(targetDeckId)
                // Fast Mode judgment is same as the actual flow (Req18.2). In E2E environments
                // where the model file is placed in DEBUG, this becomes true, skipping preview.
                modelRepository.checkModelStatus(ModelConfig.GEMMA_4_E2B)
                val modelReady = modelRepository.modelState.value is ModelState.Ready
                if (modelReady) {
                    android.util.Log.d("ScanVM", "Fast mode: model ready — starting extraction directly")
                    backgroundTaskManager.startExtraction(targetDeckId, ModelConfig.DEFAULT_ID)
                }
                onComplete(targetDeckId, modelReady)
            } catch (e: Exception) {
                android.util.Log.e("ScanVM", "Dummy insert failed", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

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
        val wall = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        android.util.Log.d("ScanVM", "processScansWithPipeline start wall=$wall deckId=$deckId pages=${_scannedPages.value.size}")
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            _isProcessing.value = true
            try {
                var tCreate = 0L
                val targetDeckId = if (deckId <= 0) {
                    val tc0 = android.os.SystemClock.elapsedRealtime()
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val id = manageDeckUseCase.createDeck("Scan $dateStr")
                    tCreate = android.os.SystemClock.elapsedRealtime() - tc0
                    android.util.Log.d("ScanVM", "processScansWithPipeline createDeck id=$id took=${tCreate}ms")
                    id
                } else deckId
                val tPipe0 = android.os.SystemClock.elapsedRealtime()
                val cards = ocrWordPipeline.execute(targetDeckId, _scannedPages.value)
                val tPipe = android.os.SystemClock.elapsedRealtime() - tPipe0
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.d("ScanVM", "processScansWithPipeline done deck=$targetDeckId cards=${cards.size} pipe=${tPipe}ms createDeck=${tCreate}ms total=${total}ms wall=$wall")
                onComplete(targetDeckId, cards.isNotEmpty())
            } catch (e: Exception) {
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.e("ScanVM", "Pipeline failed after ${total}ms wall=$wall", e)
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
        val wall = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        android.util.Log.d("ScanVM", "processScans start wall=$wall deckId=$deckId pages=${_scannedPages.value.size}")
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            _isProcessing.value = true
            try {
                var tCreate = 0L
                val targetDeckId = if (deckId <= 0) {
                    val tc0 = android.os.SystemClock.elapsedRealtime()
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val newId = manageDeckUseCase.createDeck("Scan $dateStr")
                    tCreate = android.os.SystemClock.elapsedRealtime() - tc0
                    android.util.Log.d("ScanVM", "Created new deck id=$newId took=${tCreate}ms wall=$wall")
                    newId
                } else {
                    deckId
                }
                val tOcr0 = android.os.SystemClock.elapsedRealtime()
                val scans = scanDocumentUseCase.processScannedPages(targetDeckId, _scannedPages.value)
                val tOcr = android.os.SystemClock.elapsedRealtime() - tOcr0
                android.util.Log.d("ScanVM", "processScans OCR done scans=${scans.size} took=${tOcr}ms wall=$wall deck=$targetDeckId")

                val tChk0 = android.os.SystemClock.elapsedRealtime()
                modelRepository.checkModelStatus(ModelConfig.GEMMA_4_E2B)
                val modelReady = modelRepository.modelState.value is ModelState.Ready
                val tChk = android.os.SystemClock.elapsedRealtime() - tChk0
                android.util.Log.d("ScanVM", "processScans modelCheck ready=$modelReady took=${tChk}ms wall=$wall")
                if (modelReady) {
                    val tEnq0 = android.os.SystemClock.elapsedRealtime()
                    android.util.Log.d("ScanVM", "Fast mode: model ready — starting extraction directly wall=$wall")
                    backgroundTaskManager.startExtraction(targetDeckId, ModelConfig.DEFAULT_ID)
                    val tEnq = android.os.SystemClock.elapsedRealtime() - tEnq0
                    android.util.Log.d("ScanVM", "processScans startExtraction done took=${tEnq}ms wall=$wall")
                }
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.d("ScanVM", "processScans done deck=$targetDeckId fastMode=$modelReady total=${total}ms wall=$wall create=${tCreate}ms ocr=${tOcr}ms")
                onComplete(targetDeckId, modelReady)
            } catch (e: Exception) {
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.e("ScanVM", "Error processing scans after ${total}ms wall=$wall", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun insertDummyScanForE2E(deckId: Long, onComplete: (Long, Boolean) -> Unit) {
        val wall = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        android.util.Log.d("ScanVM", "insertDummy start wall=$wall deckId=$deckId")
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
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
                    android.util.Log.d("ScanVM", "Fast mode dummy: model ready — starting extraction wall=$wall deck=$targetDeckId")
                    backgroundTaskManager.startExtraction(targetDeckId, ModelConfig.DEFAULT_ID)
                }
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.d("ScanVM", "insertDummy done deck=$targetDeckId ready=$modelReady total=${total}ms wall=$wall")
                onComplete(targetDeckId, modelReady)
            } catch (e: Exception) {
                val total = android.os.SystemClock.elapsedRealtime() - t0
                android.util.Log.e("ScanVM", "Dummy insert failed after ${total}ms wall=$wall", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

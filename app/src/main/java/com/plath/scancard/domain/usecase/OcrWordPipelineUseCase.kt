package com.plath.scancard.domain.usecase

import android.net.Uri
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.data.ml.TextRecognitionManager
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.repository.ScanRepository
import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.PromptValidator
import com.plath.scancard.domain.util.TranslationPromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface IWordGenerationExtractor {
    suspend fun isReady(): Boolean
    suspend fun generateWords(ocrText: String): List<com.plath.scancard.data.ml.ExtractedCard>
}

class GemmaWordGenerationExtractor @Inject constructor(
    internal val extractor: GemmaCardExtractor,
    private val promptBuilder: TranslationPromptBuilder,
    private val validator: PromptValidator,
) : IWordGenerationExtractor {
    override suspend fun isReady(): Boolean = true
    suspend fun initialize(modelPath: String) = extractor.initialize(modelPath)
    override suspend fun generateWords(ocrText: String): List<com.plath.scancard.data.ml.ExtractedCard> {
        if (ocrText.isBlank()) return emptyList()
        var prompt = promptBuilder.buildPrompt(ocrText)
        var pairs = extractor.extractCards(prompt)
        var allValid = pairs.all { validator.isValid(it.term, it.definition) }
        if (!allValid && pairs.isNotEmpty()) {
            val failed = pairs.find { !validator.isValid(it.term, it.definition) }?.term ?: ""
            prompt = promptBuilder.buildImprovedPrompt(ocrText, failed)
            pairs = extractor.extractCards(prompt)
        }
        return pairs.filter { validator.isValid(it.term, it.definition) }
    }
}

sealed interface OcrPipelineState {
    data object Idle : OcrPipelineState
    data class Ocring(val current: Int, val total: Int) : OcrPipelineState
    data class Generating(val current: Int, val total: Int) : OcrPipelineState
    data class Completed(val cards: List<Card>, val ocrTexts: List<String>) : OcrPipelineState
    data class Failed(val reason: String) : OcrPipelineState
}

@Singleton
class OcrWordPipelineUseCase @Inject constructor(
    private val ocr: TextRecognitionManager,
    private val wordGen: GemmaWordGenerationExtractor,
    private val cardRepository: CardRepository,
    private val scanRepository: ScanRepository,
    private val deckRepository: DeckRepository,
    private val modelRepository: ModelRepository,
    private val cardValidator: CardValidator,
    private val backgroundTaskManager: BackgroundTaskManager,
) {
    private val _state = MutableStateFlow<OcrPipelineState>(OcrPipelineState.Idle)
    val state: StateFlow<OcrPipelineState> = _state

    suspend fun execute(deckId: Long, imageUris: List<Uri>, modelConfig: ModelConfig = ModelConfig.GEMMA_4_E2B): List<Card> {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d("OcrPipeline", "execute start wall=$wall deck=$deckId pages=${imageUris.size} model=${modelConfig.id}")
        if (imageUris.isEmpty()) {
            _state.value = OcrPipelineState.Completed(emptyList(), emptyList())
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
            android.util.Log.d("OcrPipeline", "execute empty wall=$wall took=${android.os.SystemClock.elapsedRealtime()-t0}ms")
            return emptyList()
        }
        _state.value = OcrPipelineState.Ocring(0, imageUris.size)
        val tInit0 = android.os.SystemClock.elapsedRealtime()
        val modelPath = try { modelRepository.getModelPath(modelConfig) } catch (_: Exception) { null }
        modelPath?.let {
            try {
                val f = java.io.File(it)
                if (f.exists()) wordGen.initialize(it)
            } catch (e: Exception) { android.util.Log.w("OcrPipeline", "wordGen initialize failed", e) }
        }
        val tInit = android.os.SystemClock.elapsedRealtime() - tInit0
        android.util.Log.d("OcrPipeline", "execute init wall=$wall took=${tInit}ms path=$modelPath")

        val ocrTexts = mutableListOf<String>()
        val allPairs = mutableListOf<com.plath.scancard.data.ml.ExtractedCard>()

        for ((idx, uri) in imageUris.withIndex()) {
            _state.value = OcrPipelineState.Ocring(idx + 1, imageUris.size)
            val tOcr0 = android.os.SystemClock.elapsedRealtime()
            val text = try { ocr.recognizeText(uri) } catch (e: Exception) { android.util.Log.e("OcrPipeline", "ocr failed page ${idx+1}", e); "" }
            val tOcr = android.os.SystemClock.elapsedRealtime() - tOcr0
            android.util.Log.d("OcrPipeline", "execute ocr page ${idx+1}/${imageUris.size} wall=$wall took=${tOcr}ms len=${text.length} uri=$uri")
            ocrTexts.add(text)
            // persist scan
            try {
                scanRepository.insertScan(Scan(deckId = deckId, imagePath = uri.toString(), rawText = text))
            } catch (_: Exception) {}

            if (text.isBlank()) {
                android.util.Log.d("OcrPipeline", "execute skip blank page ${idx+1} wall=$wall")
                continue
            }

            _state.value = OcrPipelineState.Generating(idx + 1, imageUris.size)
            val tGen0 = android.os.SystemClock.elapsedRealtime()
            val pairs = try { wordGen.generateWords(text) } catch (e: Exception) { android.util.Log.e("OcrPipeline", "generateWords failed page ${idx+1}", e); emptyList() }
            val tGen = android.os.SystemClock.elapsedRealtime() - tGen0
            android.util.Log.d("OcrPipeline", "execute generate page ${idx+1}/${imageUris.size} wall=$wall took=${tGen}ms pairs=${pairs.size}")
            allPairs += pairs
        }

        val tPersist0 = android.os.SystemClock.elapsedRealtime()
        val cards = allPairs.mapNotNull { pair ->
            val existing = cardValidator.findDuplicate(deckId, pair.term)
            if (existing != null && existing.definition == pair.definition) null else Card(
                deckId = deckId,
                term = pair.term,
                definition = pair.definition,
                japaneseTranslation = pair.japaneseTranslation
            )
        }
        if (cards.isNotEmpty()) cardRepository.insertCards(cards)
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
        val tPersist = android.os.SystemClock.elapsedRealtime() - tPersist0
        _state.value = OcrPipelineState.Completed(cards, ocrTexts)
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("OcrPipeline", "execute done wall=$wall deck=$deckId cards=${cards.size} persist=${tPersist}ms total=${total}ms init=${tInit}ms")
        return cards
    }

    suspend fun enqueueBackground(deckId: Long, imageUris: List<Uri>, modelConfig: ModelConfig = ModelConfig.GEMMA_4_E2B) {
        val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d("OcrPipeline", "enqueueBackground start wall=$wall deck=$deckId pages=${imageUris.size}")
        for ((idx, uri) in imageUris.withIndex()) {
            val tOcr0 = android.os.SystemClock.elapsedRealtime()
            try {
                val text = ocr.recognizeText(uri)
                scanRepository.insertScan(Scan(deckId = deckId, imagePath = uri.toString(), rawText = text))
                val tOcr = android.os.SystemClock.elapsedRealtime() - tOcr0
                android.util.Log.d("OcrPipeline", "enqueueBackground ocr page ${idx+1} took=${tOcr}ms wall=$wall")
            } catch (e: Exception) { android.util.Log.e("OcrPipeline", "enqueueBackground ocr failed page ${idx+1}", e) }
        }
        backgroundTaskManager.startExtraction(deckId, modelConfig.id)
        val total = android.os.SystemClock.elapsedRealtime() - t0
        android.util.Log.d("OcrPipeline", "enqueueBackground done wall=$wall total=${total}ms")
    }
}

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
        if (imageUris.isEmpty()) {
            _state.value = OcrPipelineState.Completed(emptyList(), emptyList())
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
            return emptyList()
        }
        _state.value = OcrPipelineState.Ocring(0, imageUris.size)
        val modelPath = try { modelRepository.getModelPath(modelConfig) } catch (_: Exception) { null }
        modelPath?.let {
            try {
                val f = java.io.File(it)
                if (f.exists()) wordGen.initialize(it)
            } catch (_: Exception) {}
        }

        val ocrTexts = mutableListOf<String>()
        val allPairs = mutableListOf<com.plath.scancard.data.ml.ExtractedCard>()

        for ((idx, uri) in imageUris.withIndex()) {
            _state.value = OcrPipelineState.Ocring(idx + 1, imageUris.size)
            val text = try { ocr.recognizeText(uri) } catch (e: Exception) { "" }
            ocrTexts.add(text)
            // persist scan
            try {
                scanRepository.insertScan(Scan(deckId = deckId, imagePath = uri.toString(), rawText = text))
            } catch (_: Exception) {}

            if (text.isBlank()) continue

            _state.value = OcrPipelineState.Generating(idx + 1, imageUris.size)
            val pairs = try { wordGen.generateWords(text) } catch (_: Exception) { emptyList() }
            allPairs += pairs
        }

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
        _state.value = OcrPipelineState.Completed(cards, ocrTexts)
        return cards
    }

    suspend fun enqueueBackground(deckId: Long, imageUris: List<Uri>, modelConfig: ModelConfig = ModelConfig.GEMMA_4_E2B) {
        for (uri in imageUris) {
            try {
                val text = ocr.recognizeText(uri)
                scanRepository.insertScan(Scan(deckId = deckId, imagePath = uri.toString(), rawText = text))
            } catch (_: Exception) {}
        }
        backgroundTaskManager.startExtraction(deckId, modelConfig.id)
    }
}

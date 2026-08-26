package com.plath.scancard.domain.usecase

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.repository.ScanRepository
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.PromptValidator
import com.plath.scancard.domain.util.TranslationPromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExtractCardsUseCase @Inject constructor(
    private val cardExtractor: GemmaCardExtractor,
    private val scanRepository: ScanRepository,
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val modelRepository: ModelRepository,
    private val promptBuilder: TranslationPromptBuilder,
    private val promptValidator: PromptValidator,
    private val cardValidator: CardValidator
) {
    /**
     * スキャンを1枚ずつ処理し、ページ単位の進捗を onProgress に報告する（Req12.13）。
     * onProgress: (current, total) — 最初のページ処理前に (0, N)、各ページ完了直後に (i, N)。
     */
    suspend fun extractAndSaveCards(
        deckId: Long,
        modelConfig: ModelConfig,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val modelPath = modelRepository.getModelPath(modelConfig)
            ?: throw IllegalStateException("Model ${modelConfig.name} not found. Please download it first.")

        cardExtractor.initialize(modelPath)

        val scans = scanRepository.getScansByDeck(deckId).first()
        val combinedText = scans.joinToString("\n") { it.rawText }

        if (combinedText.isBlank()) {
            // 抽出対象が空でもデッキの状態は完了にしておかないとレジュームが無限ループする（Req12.8）
            deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
            return
        }

        val total = scans.size
        val extractedPairs = mutableListOf<com.plath.scancard.data.ml.ExtractedCard>()
        onProgress(0, total)

        // 手動テスト: repeat+return@repeat はbreakせず3回常に実行されメモリ増(8.6→9.5GB)を招く。for+breakに修正
        for ((index, scan) in scans.withIndex()) {
            val pageText = scan.rawText
            var pagePairs = emptyList<com.plath.scancard.data.ml.ExtractedCard>()
            var currentPrompt = promptBuilder.buildPrompt(pageText)

            for (attempt in 0 until 3) {
                pagePairs = cardExtractor.extractCards(currentPrompt)

                val allValid = pagePairs.all {
                    promptValidator.isValid(it.term, it.definition)
                }

                if (allValid && pagePairs.isNotEmpty()) {
                    break
                }

                if (attempt < 2 && pagePairs.isNotEmpty()) {
                    val failedTerm = pagePairs.find { !promptValidator.isValid(it.term, it.definition) }?.term ?: ""
                    currentPrompt = promptBuilder.buildImprovedPrompt(pageText, failedTerm)
                }
            }

            extractedPairs += pagePairs
            onProgress(index + 1, total)
        }
        
        val cards = extractedPairs.mapNotNull { pair ->
            // Duplicate check
            val existing = cardValidator.findDuplicate(deckId, pair.term)
            if (existing != null && existing.definition == pair.definition) {
                return@mapNotNull null // Skip exact duplicates
            }
            
            Card(
                deckId = deckId,
                term = pair.term,
                definition = pair.definition,
                japaneseTranslation = pair.japaneseTranslation
            )
        }
        
        cardRepository.insertCards(cards)
        // カード永続化と完了マークを worker の success 報告前に完了させる（Req12.8）。
        // ここが失敗すれば worker は success を返さず、ステータスも COMPLETED にならない。
        deckRepository.updateExtractionStatus(deckId, ExtractionStatus.COMPLETED)
    }
}

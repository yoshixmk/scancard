package com.plath.scancard.domain.usecase

import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.data.ml.ExtractedCard
import com.plath.scancard.data.ml.GemmaCardExtractor
import com.plath.scancard.domain.model.ExtractionStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.repository.CardRepository
import com.plath.scancard.domain.repository.DeckRepository
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.repository.ScanRepository
import com.plath.scancard.domain.util.CardValidator
import com.plath.scancard.domain.util.TranslationPromptBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for blank-text skip in ExtractCardsUseCase (ocr-pipeline Req2.3/Req3.4).
 *
 * - Blank deck completes without model init or LLM call.
 * - Blank pages (covers, dividers) skip LLM but still advance progress.
 */
class ExtractCardsUseCaseTest {

    private val cardExtractor: GemmaCardExtractor = mockk(relaxed = true)
    private val scanRepository: ScanRepository = mockk()
    private val cardRepository: CardRepository = mockk(relaxed = true)
    private val deckRepository: DeckRepository = mockk(relaxed = true)
    private val modelRepository: ModelRepository = mockk()
    private val cardValidator: CardValidator = mockk()

    private fun useCase() = ExtractCardsUseCase(
        cardExtractor,
        scanRepository,
        cardRepository,
        deckRepository,
        modelRepository,
        TranslationPromptBuilder(),
        cardValidator
    )

    private fun scan(id: Long, text: String) =
        Scan(id = id, deckId = 7, imagePath = "dummy://$id", rawText = text)

    @Test
    fun `blank deck completes without model init or LLM`() = runTest {
        coEvery { scanRepository.getScansByDeck(7) } returns
            flowOf(listOf(scan(1, "  "), scan(2, "")))

        useCase().extractAndSaveCards(7, ModelConfig.GEMMA_4_E2B)

        coVerify(exactly = 0) { cardExtractor.initialize(any()) }
        coVerify(exactly = 0) { cardExtractor.extractCards(any()) }
        coVerify { deckRepository.updateExtractionStatus(7, ExtractionStatus.COMPLETED) }
    }

    @Test
    fun `blank page skips LLM but advances progress`() = runTest {
        coEvery { scanRepository.getScansByDeck(7) } returns
            flowOf(listOf(scan(1, ""), scan(2, "Apple: A fruit")))
        coEvery { modelRepository.getModelPath(any()) } returns "/tmp/model"
        coEvery { cardExtractor.extractCards(any()) } returns
            listOf(ExtractedCard("Apple", "A fruit", "りんご"))
        coEvery { cardValidator.findDuplicate(any(), any()) } returns null

        val progress = mutableListOf<Pair<Int, Int>>()
        useCase().extractAndSaveCards(7, ModelConfig.GEMMA_4_E2B) { c, t ->
            progress += c to t
        }

        coVerify(exactly = 1) { cardExtractor.extractCards(any()) }
        assertThat(progress).containsExactly(0 to 2, 1 to 2, 2 to 2).inOrder()
        coVerify { cardRepository.insertCards(match { it.size == 1 && it[0].term == "Apple" }) }
        coVerify { deckRepository.updateExtractionStatus(7, ExtractionStatus.COMPLETED) }
        coVerify { cardExtractor.close() }
    }

    @Test
    fun `LLM failure closes extractor without completing`() = runTest {
        coEvery { scanRepository.getScansByDeck(7) } returns
            flowOf(listOf(scan(1, "Apple: A fruit")))
        coEvery { modelRepository.getModelPath(any()) } returns "/tmp/model"
        coEvery { cardExtractor.extractCards(any()) } throws RuntimeException("LLM error")

        var thrown: Throwable? = null
        try {
            useCase().extractAndSaveCards(7, ModelConfig.GEMMA_4_E2B)
        } catch (e: RuntimeException) {
            thrown = e
        }

        assertThat(thrown).hasMessageThat().isEqualTo("LLM error")
        coVerify { cardExtractor.close() }
        coVerify(exactly = 0) {
            deckRepository.updateExtractionStatus(7, ExtractionStatus.COMPLETED)
        }
    }

    @Test
    fun `scanIds scope extraction to new pages only`() = runTest {        coEvery { scanRepository.getScansByDeck(7) } returns
            flowOf(listOf(scan(1, "Old: page"), scan(2, "New: page")))
        coEvery { modelRepository.getModelPath(any()) } returns "/tmp/model"
        coEvery { cardExtractor.extractCards(match { it.contains("New: page") }) } returns
            listOf(ExtractedCard("New", "page", "new_ja"))
        coEvery { cardValidator.findDuplicate(any(), any()) } returns null

        useCase().extractAndSaveCards(7, ModelConfig.GEMMA_4_E2B, scanIds = listOf(2L))

        coVerify(exactly = 1) { cardExtractor.extractCards(any()) }
        coVerify { cardRepository.insertCards(match { it.size == 1 && it[0].term == "New" }) }
        coVerify { deckRepository.updateExtractionStatus(7, ExtractionStatus.COMPLETED) }
    }

    @Test
    fun `same term with different definition is still a duplicate`() = runTest {
        coEvery { scanRepository.getScansByDeck(7) } returns
            flowOf(listOf(scan(1, "page one"), scan(2, "page two")))
        coEvery { modelRepository.getModelPath(any()) } returns "/tmp/model"
        coEvery { cardExtractor.extractCards(any()) } returnsMany listOf(
            listOf(ExtractedCard("Kotlin", "aaa", "x")),
            listOf(ExtractedCard("Kotlin", "bbb", "y"))
        )
        coEvery { cardValidator.findDuplicate(any(), any()) } returns null

        useCase().extractAndSaveCards(7, ModelConfig.GEMMA_4_E2B)

        coVerify { cardRepository.insertCards(match { it.size == 1 && it[0].term == "Kotlin" && it[0].definition == "aaa" }) }
        coVerify { deckRepository.updateExtractionStatus(7, ExtractionStatus.COMPLETED) }
    }
}

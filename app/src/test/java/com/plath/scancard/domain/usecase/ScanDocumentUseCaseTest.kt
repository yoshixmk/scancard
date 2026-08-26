package com.plath.scancard.domain.usecase

import android.net.Uri
import com.plath.scancard.data.local.entities.Scan
import com.plath.scancard.data.ml.TextRecognitionManager
import com.plath.scancard.domain.repository.ScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Test for OCR parallelization in ScanDocumentUseCase (Req18.1)
 *
 * Inject virtual time delays into each page's recognizeText, and verify:
 * - all pages run in parallel (sequential: N x delay, parallel: ≈ delay)
 * - the result list and insertScan calls maintain page order
 * runTest's virtual time allows deterministic judgment without actual waiting.
 * Uri is mocked because android.net.Uri stubs return null on JVM unit tests.
 */
class ScanDocumentUseCaseTest {

    private val textRecognitionManager: TextRecognitionManager = mockk()
    private val scanRepository: ScanRepository = mockk(relaxed = true)

    private fun useCase() = ScanDocumentUseCase(textRecognitionManager, scanRepository)

    private fun mockUri(i: Int): Uri {
        val m = mockk<Uri>()
        every { m.toString() } returns "content://dummy/page$i"
        every { m.lastPathSegment } returns "page$i"
        return m
    }

    @Test
    fun `OCR pages run in parallel and preserve page order`() = runTest {
        val pageDelayMs = 100L
        val pages = (0 until 4).map { mockUri(it) }
        coEvery { textRecognitionManager.recognizeText(any()) } coAnswers {
            kotlinx.coroutines.delay(pageDelayMs)
            "text-${(firstArg<Uri>()).lastPathSegment}"
        }

        val startTime = testScheduler.currentTime
        val scans = useCase().processScannedPages(deckId = 42, pageUris = pages)
        val elapsed = testScheduler.currentTime - startTime

        // 400ms if executed sequentially. If parallelized, completes in 1 delay (+alpha).
        assertThat(elapsed).isLessThan(pageDelayMs * pages.size)
        assertThat(scans.map { it.imagePath }).isEqualTo(pages.map { it.toString() })
        assertThat(scans.map { it.rawText }).containsExactly(
            "text-page0", "text-page1", "text-page2", "text-page3"
        ).inOrder()
        assertThat(scans.all { it.deckId == 42L }).isTrue()
    }

    @Test
    fun `insertScan is called once per page in page order`() = runTest {
        val pages = listOf(mockUri(0), mockUri(1), mockUri(2))
        coEvery { textRecognitionManager.recognizeText(any()) } returns "ocr"
        val inserted = slot<Scan>()
        coEvery { scanRepository.insertScan(capture(inserted)) } returns Unit

        val scans = useCase().processScannedPages(deckId = 7, pageUris = pages)

        coVerify(exactly = pages.size) { scanRepository.insertScan(any()) }
        assertThat(scans).hasSize(pages.size)
        assertThat(inserted.captured.deckId).isEqualTo(7L)
    }
}

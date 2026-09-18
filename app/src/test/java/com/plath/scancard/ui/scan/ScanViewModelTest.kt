package com.plath.scancard.ui.scan

import android.content.Context
import com.plath.scancard.domain.model.ModelState
import com.plath.scancard.domain.repository.ModelRepository
import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.usecase.ManageDeckUseCase
import com.plath.scancard.domain.usecase.ScanDocumentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Fast Mode test for ScanViewModel (Req18.2/18.3/18.4)
 *
 * - When ModelState.Ready: startExtraction runs automatically after processScans completes,
 *   and fastMode=true is passed to onComplete (trigger to skip extraction preview).
 * - When ModelState.Idle (not downloaded): startExtraction is not called and fastMode=false
 *   (trigger to navigate to ExtractionPreviewScreen as usual).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val scanDocumentUseCase: ScanDocumentUseCase = mockk()
    private val manageDeckUseCase: ManageDeckUseCase = mockk()
    private val modelRepository: ModelRepository = mockk()
    private val backgroundTaskManager: BackgroundTaskManager = mockk()
    private val context: Context = mockk(relaxed = true)

    private lateinit var viewModel: ScanViewModel
    private val modelState = MutableStateFlow<ModelState>(ModelState.Idle)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { modelRepository.modelState } returns modelState
        coEvery { manageDeckUseCase.createDeck(any()) } returns 42L
        coEvery { scanDocumentUseCase.processScannedPages(any(), any()) } returns emptyList()
        coEvery { scanDocumentUseCase.insertDummyScan(any()) } returns 42L
        coEvery { backgroundTaskManager.startExtraction(any(), any(), any()) } returns Unit
        every { modelRepository.checkModelStatus(any()) } returns Unit
        viewModel = ScanViewModel(
            context,
            scanDocumentUseCase,
            manageDeckUseCase,
            modelRepository,
            backgroundTaskManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `model ready triggers auto extraction and reports fastMode`() = runTest(dispatcher) {
        modelState.value = ModelState.Ready
        var completed: Pair<Long, Boolean>? = null

        viewModel.processScans(0L) { deckId, fastMode -> completed = deckId to fastMode }

        coVerify(exactly = 1) {
            backgroundTaskManager.startExtraction(42L, "gemma-4-e2b", emptyList())
        }
        assertThat(completed).isEqualTo(42L to true)
    }

    @Test
    fun `model idle falls back without starting extraction`() = runTest(dispatcher) {
        modelState.value = ModelState.Idle
        var completed: Pair<Long, Boolean>? = null

        viewModel.processScans(0L) { deckId, fastMode -> completed = deckId to fastMode }

        coVerify(exactly = 0) { backgroundTaskManager.startExtraction(any(), any(), any()) }
        assertThat(completed).isEqualTo(42L to false)
    }

    @Test
    fun `extraction scoped to new scan ids and pages cleared`() = runTest(dispatcher) {
        modelState.value = ModelState.Ready
        val page: android.net.Uri = mockk()
        viewModel.addPages(listOf(page))
        coEvery { scanDocumentUseCase.processScannedPages(any(), any()) } returns
            listOf(com.plath.scancard.data.local.entities.Scan(id = 5, deckId = 42, imagePath = "x", rawText = "t"))

        viewModel.processScans(0L) { _, _ -> }

        coVerify(exactly = 1) {
            backgroundTaskManager.startExtraction(42L, "gemma-4-e2b", listOf(5L))
        }
        assertThat(viewModel.scannedPages.value).isEmpty()
    }
}

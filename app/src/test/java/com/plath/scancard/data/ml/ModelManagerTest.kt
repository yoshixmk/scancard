package com.plath.scancard.data.ml

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.aipacks.AiPackManager
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelManagerTest {

    private lateinit var context: Context
    private lateinit var aiPackManager: AiPackManager
    private lateinit var manager: ModelManager

    private val config = ModelConfig.GEMMA_4_E2B

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        val tmpDir = Files.createTempDirectory("scancard-test").toFile()
        tmpDir.deleteOnExit()
        every { context.filesDir } returns tmpDir
        every { context.packageName } returns "com.plath.scancard"

        aiPackManager = mockk(relaxed = true)
    }

    @Test
    fun `checkModelStatus with AiPack failure in DEBUG should be Idle not Error`() {
        every { aiPackManager.getPackStates(listOf(config.aiPackName)) } returns Tasks.forException(RuntimeException("AssetPackService onError(-1)"))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.checkModelStatus(config)

        Thread.sleep(200)
        val state = manager.modelState.value
        assertTrue("Expected Idle when AiPack unavailable in DEBUG, got $state", state is ModelState.Idle)
    }

    @Test
    fun `downloadModel with fetch failure in DEBUG should be Idle`() {
        every { aiPackManager.fetch(listOf(config.aiPackName)) } returns Tasks.forException(RuntimeException("Download failed -1"))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.downloadModel(config)

        Thread.sleep(200)
        val state = manager.modelState.value
        assertTrue("Expected Idle on fetch failure in DEBUG, got $state", state is ModelState.Idle)
    }

    @Test
    fun `isModelDownloaded should not throw when getPackLocation fails`() {
        every { aiPackManager.getPackLocation(config.aiPackName) } throws RuntimeException("not found")

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        val result = manager.isModelDownloaded(config)
        assertFalse(result)
    }
}

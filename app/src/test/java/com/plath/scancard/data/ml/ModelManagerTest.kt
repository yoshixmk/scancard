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
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forException(RuntimeException("AssetPackService onError(-1)"))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.checkModelStatus(config)

        Thread.sleep(200)
        val state = manager.modelState.value
        assertTrue("Expected Idle when AiPack unavailable in DEBUG, got $state", state is ModelState.Idle)
    }

    @Test
    fun `downloadModel with fetch failure in DEBUG should be Idle`() {
        every { aiPackManager.fetch(config.aiPackNames) } returns Tasks.forException(RuntimeException("Download failed -1"))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.downloadModel(config)

        Thread.sleep(200)
        val state = manager.modelState.value
        assertTrue("Expected Idle on fetch failure in DEBUG, got $state", state is ModelState.Idle)
    }

    @Test
    fun `isModelDownloaded should not throw when getPackLocation fails`() {
        every { aiPackManager.getPackLocation(any()) } throws RuntimeException("not found")

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        val result = manager.isModelDownloaded(config)
        assertFalse(result)
    }

    @Test
    fun `assembleParts concatenates split files in order`() {
        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        val tmpDir = (context.filesDir as File).also { it.mkdirs() }
        val part0 = File(tmpDir, "p0.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val part1 = File(tmpDir, "p1.bin").apply { writeBytes(byteArrayOf(4, 5)) }
        val out = File(tmpDir, "assembled.bin").apply { if (exists()) delete() }

        val result = manager.assembleParts(listOf(part0, part1), out)
        assertNotNull(result)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), out.readBytes())
    }
}

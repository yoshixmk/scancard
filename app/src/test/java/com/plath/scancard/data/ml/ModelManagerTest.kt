package com.plath.scancard.data.ml

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.aipacks.AiPackManager
import com.google.android.play.core.aipacks.AiPackState
import com.google.android.play.core.aipacks.AiPackStates
import com.google.android.play.core.aipacks.model.AiPackStatus
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private fun mockPackState(
        name: String,
        status: Int,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 1000L,
        transferPercent: Int = 0
    ): AiPackState {
        return object : AiPackState() {
            override fun name(): String = name
            override fun status(): Int = status
            override fun bytesDownloaded(): Long = bytesDownloaded
            override fun totalBytesToDownload(): Long = totalBytes
            override fun transferProgressPercentage(): Int = transferPercent
            override fun errorCode(): Int = 0
        }
    }

    private fun mockPackStates(vararg states: AiPackState): AiPackStates {
        val map = states.associateBy { it.name() }
        return object : AiPackStates() {
            override fun totalBytes(): Long = map.values.sumOf { it.totalBytesToDownload() }
            override fun packStates(): Map<String, AiPackState> = map
        }
    }

    @Test
    fun `checkModelStatus registers listener for progress updates`() {
        val s0 = mockPackState(config.aiPackNames[0], AiPackStatus.PENDING, 0, 1000)
        val s1 = mockPackState(config.aiPackNames[1], AiPackStatus.PENDING, 0, 1000)
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forResult(mockPackStates(s0, s1))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.checkModelStatus(config)

        Thread.sleep(200)
        verify(exactly = 1) { aiPackManager.registerListener(any()) }
        assertTrue(manager.packListener != null)
        val state = manager.modelState.value
        assertTrue("Expected Downloading after PENDING, got $state", state is ModelState.Downloading)
    }

    @Test
    fun `listener callback refreshes aggregate progress instead of stuck at 0 percent`() {
        val start0 = mockPackState(config.aiPackNames[0], AiPackStatus.DOWNLOADING, 0, 1000)
        val start1 = mockPackState(config.aiPackNames[1], AiPackStatus.DOWNLOADING, 0, 1000)
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forResult(mockPackStates(start0, start1))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.checkModelStatus(config)
        Thread.sleep(200)
        val initial = manager.modelState.value as? ModelState.Downloading
        assertNotNull(initial)
        assertEquals(0f, initial!!.progress, 0.001f)

        // Simulate download advancing to 50%: next getPackStates returns progressed snapshot.
        val prog0 = mockPackState(config.aiPackNames[0], AiPackStatus.DOWNLOADING, 500, 1000)
        val prog1 = mockPackState(config.aiPackNames[1], AiPackStatus.DOWNLOADING, 500, 1000)
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forResult(mockPackStates(prog0, prog1))

        val push = mockPackState(config.aiPackNames[0], AiPackStatus.DOWNLOADING, 500, 1000)
        manager.packListener!!.onStateUpdate(push)
        Thread.sleep(200)

        val updated = manager.modelState.value as? ModelState.Downloading
        assertNotNull("Expected Downloading after listener update, got ${manager.modelState.value}", updated)
        assertEquals(0.5f, updated!!.progress, 0.001f)
    }

    @Test
    fun `TRANSFERRING and WAITING_FOR_WIFI stay Downloading not Idle`() {
        val t0 = mockPackState(config.aiPackNames[0], AiPackStatus.TRANSFERRING, 900, 1000)
        val t1 = mockPackState(config.aiPackNames[1], AiPackStatus.DOWNLOADING, 900, 1000)
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forResult(mockPackStates(t0, t1))

        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        manager.checkModelStatus(config)
        Thread.sleep(200)
        val transferring = manager.modelState.value
        assertTrue("TRANSFERRING should be Downloading, got $transferring", transferring is ModelState.Downloading)

        val w0 = mockPackState(config.aiPackNames[0], AiPackStatus.WAITING_FOR_WIFI, 100, 1000)
        val w1 = mockPackState(config.aiPackNames[1], AiPackStatus.DOWNLOADING, 100, 1000)
        every { aiPackManager.getPackStates(config.aiPackNames) } returns Tasks.forResult(mockPackStates(w0, w1))
        manager.checkModelStatus(config)
        Thread.sleep(200)
        val waiting = manager.modelState.value
        assertTrue("WAITING_FOR_WIFI should stay Downloading, got $waiting", waiting is ModelState.Downloading)
    }

    @Test
    fun `computeAggregateProgress preserves previous progress when total unknown`() {
        manager = ModelManager(context)
        manager.aiPackManager = aiPackManager
        val known0 = mockPackState(config.aiPackNames[0], AiPackStatus.DOWNLOADING, 300, 1000)
        val known1 = mockPackState(config.aiPackNames[1], AiPackStatus.DOWNLOADING, 300, 1000)
        manager.handlePackStates(config, listOf(known0, known1))
        val before = (manager.modelState.value as ModelState.Downloading).progress
        assertEquals(0.3f, before, 0.001f)

        val unknown0 = mockPackState(config.aiPackNames[0], AiPackStatus.PENDING, 0, 0, 0)
        val unknown1 = mockPackState(config.aiPackNames[1], AiPackStatus.PENDING, 0, 0, 0)
        val preserved = manager.computeAggregateProgress(listOf(unknown0, unknown1))
        assertEquals(0.3f, preserved, 0.001f)
    }
}

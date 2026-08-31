package com.plath.scancard.data.ml

import android.content.Context
import com.google.ai.edge.litertlm.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * IMP-10 10-1 Audit results (2026-08-23) — Comparison with camerax/SKILL.md threading / immutability / testing
 *
 * [Threading / Contention] Note (existing behavior maintained, comments only):
 *   - engine / conversation are mutable nullable vars read/written within withContext(Dispatchers.IO).
 *     initialize()'s "if (engine != null && conversation != null) return" is non-atomic (TOCTOU).
 *     If two coroutines call initialize() simultaneously, Engine might be double-created.
 *     Countermeasure: Guard with Mutex or use @Singleton scope + synchronized. Currently
 *     ScanDocumentUseCase calls serially, so actual harm is minor, but fix is required for future
 *     parallel extraction.
 *   - extractCards()'s "val currentConversation = conversation ?: return emptyList()" is OK as it
 *     takes a snapshot. However, if close() is called from another thread, currentConversation
 *     might be already closed (contention). Countermeasure: Synchronize close() with Mutex,
 *     or check isClosed within extractCards.
 *   - EngineConfig / SamplerConfig / ConversationConfig are equivalent to data class builders,
 *     and Engine(engineConfig).initialize() is a suspend-like initialization with side effects —
 *     correctly executed on Dispatchers.IO.
 *
 * [Escaping / Resources] Note:
 *   - callback in conversation.sendMessageAsync(input, MessageCallback) is a closure that escapes
 *     to suspendCancellableCoroutine's continuation. Guarded with isActive against risk of
 *     double-calling continuation.resume, so it's OK. However, LiteRT-side inference is not
 *     canceled upon coroutine cancellation (LiteRT has no cancel API). Countermeasure: Add
 *     "continuation.invokeOnCancellation { /* call if interruption possible on conversation side */ }".
 *     Currently commented out as LiteRT 0.16.1 lacks a cancel API.
 *   - close() performs native release via conversation?.close() / engine?.close(), but if an
 *     exception occurs before null-clearing, one might remain open. Improvement: close both
 *     in try/finally.
 *
 * [Immutability / Builder reassignment] OK:
 *   - EngineConfig(modelPath, Backend.CPU(), maxNumTokens) / SamplerConfig(topK, topP, temp) are
 *     immutable via constructor; doesn't fall into Builder's fluent reassignment pattern.
 *   - StringBuilder is thread-unsafe, but as MessageCallback.onMessage is expected to be called
 *     sequentially from LiteRT's single thread (executor), there's no actual harm. Consider
 *     replacing with StringBuffer if called in parallel.
 *   - No "discarding return value" omissions like PendingRecording.withAudioEnabled() in
 *     camerax/references/immutability.md.
 *
 * [Testing] — Created FakeGemmaExtractor in app/src/test/fakes/FakeGemmaExtractor.kt following this audit.
 *   Allows testing of ScanDocumentUseCase's asynchronous lifecycle using Fakes instead of Mockito
 *   (adheres to camerax/testing.md). Since this class is an on-device LLM, it cannot be initialized
 *   in Robolectric — test via delegation to Fake.
 *
 * [Thermal] — LiteRT inference is heavy on CPU/GPU. Refer to designs for reducing token counts
 *   via postponing inference or resolution downgrade -> OCR character reduction in
 *   docs/camera-thermals.md's Severe tier.
 */
class GemmaCardExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: CardResponseParser
) {
    // AUDIT NOTE: Mutable state requiring contention measures — activate the following if guarding with Mutex in the future:
    // private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isDummyMode = false

    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        if (isDummyMode) {
            return@withContext
        }
        if (engine != null && conversation != null) {
            return@withContext
        }

        try {
            // E2E dummy mode: small file (<5MB) in DEBUG avoids 2.6GB load and 30s inference, returns dummy cards quickly
            val f = java.io.File(modelPath)
            if (com.plath.scancard.BuildConfig.DEBUG && f.exists() && f.length() < 5 * 1024 * 1024) {
                isDummyMode = true
                return@withContext
            }
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                maxNumTokens = 4096
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()
            
            val samplerConfig = SamplerConfig(
                topK = 40,
                topP = 0.95,
                temperature = 0.2
            )
            
            val newConversation = newEngine.createConversation(
                ConversationConfig(samplerConfig = samplerConfig)
            )
            
            engine = newEngine
            conversation = newConversation
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun extractCards(text: String): List<ExtractedCard> = withContext(Dispatchers.IO) {
        if (isDummyMode) {
            // E2E fast path: simulate 8s inference and return parsed dummy.
            // 8s: To ensure a window where E2E can reliably poll the "Page n of m" progress in
            // the notification area, considering emulator clock drift/polling intervals (Req12.12)
            kotlinx.coroutines.delay(8000)
            return@withContext listOf(
                ExtractedCard(term = "Apple", definition = "A fruit", japaneseTranslation = "apple_ja"),
                ExtractedCard(term = "Banana", definition = "Yellow fruit", japaneseTranslation = "banana_ja")
            )
        }
        val currentConversation = conversation ?: run {
            return@withContext emptyList()
        }

        val prompt = """
            Extract flashcard pairs (term and definition) from the following text.
            Return ONLY a JSON array of objects with "term" and "definition" keys.
            Text: $text
        """.trimIndent()

        val input = Contents.of(listOf(Content.Text(prompt)))
        
        val fullResponse = suspendCancellableCoroutine<String> { continuation ->
            val responseBuilder = StringBuilder()
            
            currentConversation.sendMessageAsync(
                input,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        responseBuilder.append(message.toString())
                    }

                    override fun onDone() {
                        if (continuation.isActive) {
                            continuation.resume(responseBuilder.toString())
                        }
                    }

                    override fun onError(throwable: Throwable) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(throwable)
                        }
                    }
                }
            )
        }

        val parsed = parser.parse(fullResponse)
        parsed
    }
    
    fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        isDummyMode = false
    }
}

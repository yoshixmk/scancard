package com.plath.scancard.fakes

import com.plath.scancard.data.ml.CardResponseParser
import com.plath.scancard.data.ml.ExtractedCard

/**
 * FakeGemmaExtractor — IMP-10 10-4
 *
 * Purpose: Fake replacement for GemmaCardExtractor. LiteRT (litertlm) is native + large-scale model
 * and cannot be initialized in Robolectric / JVM tests (model files are ~several GBs, Engine
 * initialization is heavy). This Fake reproduces CardResponseParser behavior in memory,
 * allowing testing of ScanDocumentUseCase's asynchronous lifecycle (initialize -> extractCards -> close)
 * without Mockito.
 *
 * Adheres to: `.kiro/skills/camerax/references/testing.md` Fakes over mocks
 *       - Mocking ImageProxy / Engine with Mockito becomes brittle, so use Fakes for state verification.
 *       - Uses Google Truth assertions.
 *       - Specifies @RunWith(AndroidJUnit4::class) (see 10-5).
 *
 * Usage:
 * ```
 * @RunWith(AndroidJUnit4::class)
 * class ExtractCardsUseCaseTest {
 *     private val parser = CardResponseParser()
 *     private val fakeExtractor = FakeGemmaExtractor(parser)
 *
 *     @Test fun extractCards_success() = runTest {
 *         fakeExtractor.setNextCards(listOf(ExtractedCard("Hello", "Greeting")))
 *         fakeExtractor.initialize("/fake/model/path") // Immediately succeeds as it's a Fake
 *         val result = fakeExtractor.extractCards("Hello means Greeting")
 *         assertThat(result).hasSize(1) // Truth
 *         assertThat(result[0].term).isEqualTo("Hello")
 *     }
 *
 *     @Test fun extractCards_error() = runTest {
 *         fakeExtractor.setNextError(RuntimeException("LLM error"))
 *         assertThrows(RuntimeException::class.java) { fakeExtractor.extractCards("text") }
 *     }
 *
 *     @Test fun initialize_idempotent() = runTest {
 *         fakeExtractor.initialize("/fake/path")
 *         fakeExtractor.initialize("/fake/path") // Second call is no-op (Already initialized)
 *         assertThat(fakeExtractor.initializeCallCount).isEqualTo(2)
 *         assertThat(fakeExtractor.isInitialized).isTrue()
 *     }
 * }
 * ```
 */

// Fake that mimics GemmaCardExtractor behavior
class FakeGemmaExtractor(
    private val parser: CardResponseParser = CardResponseParser()
) {
    // --- Internal state (corresponds to actual engine/conversation) ---

    var isInitialized: Boolean = false
        private set
    var initializeCallCount: Int = 0
        private set
    var extractCallCount: Int = 0
        private set
    var closeCallCount: Int = 0
        private set
    var lastModelPath: String? = null
        private set
    var lastExtractText: String? = null
        private set

    // Cards to return in the next extractCards (success case)
    private var nextCards: List<ExtractedCard>? = null

    // Raw JSON string to return in the next extractCards (case where it's converted to ExtractedCard via parser)
    private var nextRawResponse: String? = null

    // Exception to throw in the next extractCards
    private var nextError: Throwable? = null

    // Exception to simulate initialization failure
    private var nextInitError: Throwable? = null

    // Simulate delay (for asynchronous lifecycle testing, ms)
    var fakeDelayMs: Long = 0

    // --- Operation APIs from tests (given) ---

    /** Directly specify cards to return in the next extractCards */
    fun setNextCards(cards: List<ExtractedCard>) {
        nextCards = cards
        nextRawResponse = null
        nextError = null
    }

    /** Specify raw response (JSON) for the next extractCards — when you want to verify via parser.parse() */
    fun setNextRawResponse(rawJson: String) {
        nextRawResponse = rawJson
        nextCards = null
        nextError = null
    }

    /** Set to throw an exception in the next extractCards */
    fun setNextError(throwable: Throwable) {
        nextError = throwable
        nextCards = null
        nextRawResponse = null
    }

    /** Set to throw an exception in the next initialize */
    fun setNextInitError(throwable: Throwable) {
        nextInitError = throwable
    }

    // --- Fake implementation (same signature as actual GemmaCardExtractor) ---

    /**
     * Fake initialize — simulates actual Engine(engineConfig).initialize() + createConversation
     * equivalent with immediate success. Second call onwards is no-op as "Already initialized".
     *
     * Threads: withContext(Dispatchers.IO) is only for the actual one; Fake executes immediately
     * on the calling thread. Since tests call it via runTest, dispatcher control is possible.
     */
    suspend fun initialize(modelPath: String) {
        initializeCallCount++
        lastModelPath = modelPath
        if (nextInitError != null) {
            val e = nextInitError!!
            nextInitError = null
            throw e
        }
        if (isInitialized) {
            // Actual: Log.d("GemmaExtractor", "Already initialized") + return
            return
        }
        if (fakeDelayMs > 0) kotlinx.coroutines.delay(fakeDelayMs)
        isInitialized = true
    }

    /**
     * Fake extractCards — synchronously simulates actual conversation.sendMessageAsync +
     * suspendCancellableCoroutine equivalent. Returns emptyList() if uninitialized.
     *
     * @param prompt Full prompt built by TranslationPromptBuilder
     * @return ExtractedCard list
     */
    suspend fun extractCards(prompt: String): List<ExtractedCard> {
        extractCallCount++
        lastExtractText = prompt
        if (!isInitialized) return emptyList() // Actual: conversation == null -> emptyList()

        nextError?.let { e ->
            nextError = null
            throw e
        }

        if (fakeDelayMs > 0) kotlinx.coroutines.delay(fakeDelayMs)

        // Priority: cards directly specified by setNextCards
        nextCards?.let { cards ->
            nextCards = null
            return cards
        }

        // Next: JSON specified by setNextRawResponse parsed by parser (via actual parser.parse(fullResponse))
        nextRawResponse?.let { raw ->
            nextRawResponse = null
            return parser.parse(raw)
        }

        // Default: empty list (safety measure if setNext* is forgotten in test)
        return emptyList()
    }

    /**
     * Fake close — actual conversation?.close() / engine?.close() equivalent.
     * Safe to call multiple times (idempotent).
     */
    fun close() {
        closeCallCount++
        isInitialized = false
    }

    /** Reset state between tests — call in @Before / @After */
    fun reset() {
        isInitialized = false
        initializeCallCount = 0
        extractCallCount = 0
        closeCallCount = 0
        lastModelPath = null
        lastExtractText = null
        nextCards = null
        nextRawResponse = null
        nextError = null
        nextInitError = null
        fakeDelayMs = 0
    }

    // --- Truth assertions substitution examples (see docs/testing.md 10-5) ---
    // JUnit: assertEquals(true, fake.isInitialized)
    // Truth: assertThat(fake.isInitialized).isTrue()
    // JUnit: assertEquals(1, result.size)
    // Truth: assertThat(result).hasSize(1)
    // JUnit: assertEquals("Hello", result[0].term)
    // Truth: assertThat(result[0].term).isEqualTo("Hello")
    // JUnit: assertNotNull(result)
    // Truth: assertThat(result).isNotNull() or assertThat(result).isEmpty() / isNotEmpty()
}

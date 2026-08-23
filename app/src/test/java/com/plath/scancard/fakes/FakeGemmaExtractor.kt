package com.plath.scancard.fakes

import com.plath.scancard.data.ml.CardResponseParser
import com.plath.scancard.data.ml.ExtractedCard

/**
 * FakeGemmaExtractor — IMP-10 10-4
 *
 * 目的: GemmaCardExtractor の Fake 置換。LiteRT (litertlm) はネイティブ + 大容量モデルで
 * Robolectric / JVM テストで初期化不可 (モデルファイル ~数GB、Engine 初期化が重い)。
 * 本 Fake は CardResponseParser の振る舞いをメモリで再現し、ScanDocumentUseCase の
 * 非同期ライフサイクル (initialize -> extractCards -> close) を Mockito なしでテストする。
 *
 * 準拠: `.kiro/skills/camerax/references/testing.md` Fakes over mocks
 *       - Mockito で ImageProxy / Engine をモックすると brittle になるため Fake で状態検証。
 *       - Google Truth assertions を使用。
 *       - @RunWith(AndroidJUnit4::class) を明示 (10-5 参照)。
 *
 * 使い方:
 * ```
 * @RunWith(AndroidJUnit4::class)
 * class ExtractCardsUseCaseTest {
 *     private val parser = CardResponseParser()
 *     private val fakeExtractor = FakeGemmaExtractor(parser)
 *
 *     @Test fun extractCards_success() = runTest {
 *         fakeExtractor.setNextCards(listOf(ExtractedCard("Hello", "Greeting")))
 *         fakeExtractor.initialize("/fake/model/path") // Fake なので即成功
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
 *         fakeExtractor.initialize("/fake/path") // 2回目は no-op (本物と同様 Already initialized)
 *         assertThat(fakeExtractor.initializeCallCount).isEqualTo(2)
 *         assertThat(fakeExtractor.isInitialized).isTrue()
 *     }
 * }
 * ```
 */

// GemmaCardExtractor の振る舞いを模倣する Fake
class FakeGemmaExtractor(
    private val parser: CardResponseParser = CardResponseParser()
) {
    // --- 内部状態 (本物の engine/conversation に対応) ---

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

    // 次回 extractCards で返すカード (成功ケース)
    private var nextCards: List<ExtractedCard>? = null

    // 次回 extractCards で返す生 JSON 文字列 (parser 経由で ExtractedCard に変換されるケース)
    private var nextRawResponse: String? = null

    // 次回 extractCards で throw する例外
    private var nextError: Throwable? = null

    // 初期化失敗をシミュレートする例外
    private var nextInitError: Throwable? = null

    // 遅延をシミュレート (非同期ライフサイクルテスト用, ms)
    var fakeDelayMs: Long = 0

    // --- テストからの操作 API (given) ---

    /** 次回 extractCards で返すカードを直接指定 */
    fun setNextCards(cards: List<ExtractedCard>) {
        nextCards = cards
        nextRawResponse = null
        nextError = null
    }

    /** 次回 extractCards で返す生レスポンス (JSON) を指定 — parser.parse() を経由して検証したい場合 */
    fun setNextRawResponse(rawJson: String) {
        nextRawResponse = rawJson
        nextCards = null
        nextError = null
    }

    /** 次回 extractCards で例外を throw するように設定 */
    fun setNextError(throwable: Throwable) {
        nextError = throwable
        nextCards = null
        nextRawResponse = null
    }

    /** 次回 initialize で例外を throw するように設定 */
    fun setNextInitError(throwable: Throwable) {
        nextInitError = throwable
    }

    // --- Fake 実装 (本物の GemmaCardExtractor と同シグネチャ) ---

    /**
     * Fake initialize — 本物の Engine(engineConfig).initialize() + createConversation 相当を
     * 即時成功でシミュレート。2回目以降は本物と同様に "Already initialized" 相当で no-op。
     *
     * スレッド: withContext(Dispatchers.IO) は本物のみ、Fake は呼び出し元スレッドで即実行。
     * テストでは runTest で呼ぶため dispatcher 制御が可能。
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
            // 本物: Log.d("GemmaExtractor", "Already initialized") + return
            return
        }
        if (fakeDelayMs > 0) kotlinx.coroutines.delay(fakeDelayMs)
        isInitialized = true
    }

    /**
     * Fake extractCards — 本物の conversation.sendMessageAsync + suspendCancellableCoroutine 相当を
     * 同期的にシミュレート。未初期化なら emptyList() を返す (本物と同様)。
     *
     * @param text OCR 結果テキスト
     * @return ExtractedCard リスト
     */
    suspend fun extractCards(text: String): List<ExtractedCard> {
        extractCallCount++
        lastExtractText = text
        if (!isInitialized) return emptyList() // 本物: conversation == null -> emptyList()

        nextError?.let { e ->
            nextError = null
            throw e
        }

        if (fakeDelayMs > 0) kotlinx.coroutines.delay(fakeDelayMs)

        // 優先: setNextCards で直接指定されたカード
        nextCards?.let { cards ->
            nextCards = null
            return cards
        }

        // 次: setNextRawResponse で指定された JSON を parser でパース (本物の parser.parse(fullResponse) 経由)
        nextRawResponse?.let { raw ->
            nextRawResponse = null
            return parser.parse(raw)
        }

        // デフォルト: 空リスト (テストで setNext* し忘れた場合の安全策)
        return emptyList()
    }

    /**
     * Fake close — 本物の conversation?.close() / engine?.close() 相当。
     * 何度呼んでも安全 (idempotent)。
     */
    fun close() {
        closeCallCount++
        isInitialized = false
    }

    /** テスト間の状態リセット — @Before / @After で呼ぶ */
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

    // --- Truth assertions 置換例 (docs/testing.md 10-5 参照) ---
    // JUnit: assertEquals(true, fake.isInitialized)
    // Truth: assertThat(fake.isInitialized).isTrue()
    // JUnit: assertEquals(1, result.size)
    // Truth: assertThat(result).hasSize(1)
    // JUnit: assertEquals("Hello", result[0].term)
    // Truth: assertThat(result[0].term).isEqualTo("Hello")
    // JUnit: assertNotNull(result)
    // Truth: assertThat(result).isNotNull() または assertThat(result).isEmpty() / isNotEmpty()
}

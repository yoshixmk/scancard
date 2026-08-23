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
 * IMP-10 10-1 監査結果 (2026-08-23) — camerax/SKILL.md threading / immutability / testing 照合
 *
 * [スレッド / 競合] 要注意 (既存動作は維持、コメントのみ):
 *   - engine / conversation は mutable nullable var で withContext(Dispatchers.IO) 内で読み書き。
 *     initialize() の `if (engine != null && conversation != null) return` は非アトミック (TOCTOU)。
 *     2つのコルーチンが同時に initialize() を呼ぶと Engine が二重生成される可能性。
 *     対策案: Mutex でガードするか @Singleton スコープ + synchronized。現状は ScanDocumentUseCase が
 *     直列呼び出しのため実害小だが、将来並列抽出する際は要修正。
 *   - extractCards() の `val currentConversation = conversation ?: return emptyList()` はスナップショット取得で OK。
 *     ただし close() が別スレッドで呼ばれると currentConversation が close 済みになる競合あり。
 *     対策案: close() も Mutex で同期、または extractCards 内で isClosed チェック。
 *   - EngineConfig / SamplerConfig / ConversationConfig は data class builder 相当で、
 *     Engine(engineConfig).initialize() は副作用ありの suspend 的初期化 — Dispatchers.IO で正しく実行。
 *
 * [Escaping / リソース] 要注意:
 *   - conversation.sendMessageAsync(input, MessageCallback) の callback は suspendCancellableCoroutine の
 *     continuation にエスケープする closure。continuation.resume が2回呼ばれるリスクを isActive でガードしており OK。
 *     ただし coroutine cancel 時に LiteRT 側の推論がキャンセルされない (LiteRT は cancel API を持たない)。
 *     改善案: continuation.invokeOnCancellation { /* conversation 側で中断できれば呼ぶ */ } を追加。
 *     現状は LiteRT 0.16.1 に cancel API がないためコメント留め。
 *   - close() は conversation?.close() / engine?.close() でネイティブ解放を行うが、null クリア前に例外が出ると
 *     片方のみ close される可能性。改善案: try/finally で両方 close。
 *
 * [Immutability / Builder再代入] OK:
 *   - EngineConfig(modelPath, Backend.CPU(), maxNumTokens) / SamplerConfig(topK, topP, temp) は
 *     コンストラクタで不変、Builder の fluent 再代入パターン非該当。
 *   - StringBuilder はスレッド非安全だが MessageCallback.onMessage は LiteRT の単一スレッド(executor)から
 *     逐次呼ばれる想定のため実害なし。並列呼び出しされる場合は StringBuffer に置換を検討。
 *   - camerax/references/immutability.md の PendingRecording.withAudioEnabled() のような "戻り値捨て" 漏れは本ファイルになし。
 *
 * [Testing] — 本監査に伴い FakeGemmaExtractor を app/src/test/fakes/FakeGemmaExtractor.kt に作成。
 *   Mockito ではなく Fake で ScanDocumentUseCase の非同期ライフサイクルをテスト可能にする (camerax/testing.md 準拠)。
 *   本クラス自体は on-device LLM のため Robolectric では初期化不可 — Fake に委譲してテスト。
 *
 * [Thermal] — LiteRT 推論は CPU/GPU 高負荷。docs/camera-thermals.md の Severe tier では
 *   推論を延期 or 解像度ダウングレード→OCR文字数削減でトークン数を減らす設計を参照。
 */
class GemmaCardExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: CardResponseParser
) {
    // AUDIT NOTE: 競合対策が必要な mutable state — 将来 Mutex でガードする場合は下記を有効化:
    // private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        if (engine != null && conversation != null) {
            android.util.Log.d("GemmaExtractor", "Already initialized")
            return@withContext
        }

        try {
            android.util.Log.d("GemmaExtractor", "Initializing engine with path: $modelPath")
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
            android.util.Log.d("GemmaExtractor", "Initialization successful")
        } catch (e: Exception) {
            android.util.Log.e("GemmaExtractor", "Initialization failed", e)
            throw e
        }
    }

    suspend fun extractCards(text: String): List<ExtractedCard> = withContext(Dispatchers.IO) {
        val currentConversation = conversation ?: return@withContext emptyList()

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

        parser.parse(fullResponse)
    }
    
    fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}

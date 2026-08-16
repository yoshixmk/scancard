package com.example.scancard.data.ml

import android.content.Context
import com.google.ai.edge.litertlm.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GemmaCardExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: CardResponseParser
) {
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

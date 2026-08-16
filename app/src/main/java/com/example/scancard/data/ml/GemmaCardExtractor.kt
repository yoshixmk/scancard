package com.example.scancard.data.ml

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class GemmaCardExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: CardResponseParser
) {
    private var llmInference: LlmInference? = null

    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        if (llmInference != null) return@withContext

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setTemperature(0.2f)
            .setTopK(40)
            .build()
        
        llmInference = LlmInference.createFromOptions(context, options)
    }

    suspend fun extractCards(text: String): List<ExtractedCard> = withContext(Dispatchers.IO) {
        val prompt = """
            Extract flashcard pairs (term and definition) from the following text.
            Return ONLY a JSON array of objects with "term" and "definition" keys.
            Text: $text
        """.trimIndent()

        val response = llmInference?.generateResponse(prompt) ?: ""
        parser.parse(response)
    }
}

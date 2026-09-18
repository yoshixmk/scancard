package com.plath.scancard.domain.util


class TranslationPromptBuilder {
    fun buildPrompt(ocrText: String): String {
        return """
            Extract flashcard pairs (term and definition) from the following text.
            For each card, provide:
            1. term: The key concept or word.
            2. definition: A clear, concise English explanation.
            3. japaneseTranslation: A natural Japanese translation of the definition.
            
            Return ONLY a JSON array of objects with "term", "definition", and "japaneseTranslation" keys.
            Example: [{"term":"Photosynthesis","definition":"Process by which plants convert light into chemical energy","japaneseTranslation":"光合成"}]
            IMPORTANT: Ensure the definition is meaningful and specifically describes the term.
            Text: $ocrText
        """.trimIndent()
    }
}

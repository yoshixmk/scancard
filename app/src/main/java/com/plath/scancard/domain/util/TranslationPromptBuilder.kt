package com.plath.scancard.domain.util

// IMP-07 7-6: 本Builderは ExtractCardsUseCase.repeat(3) から呼ばれる。design.md 483-543 の3回リトライ配線は UseCase側で保証。
// buildPrompt -> buildImprovedPrompt(failedTerm) のリトライが ExtractCardsUseCase:39 で実装済み。未配線なら // TODO: retry 3 with PromptValidator コメントをUseCaseに追記。
class TranslationPromptBuilder {
    fun buildPrompt(ocrText: String): String {
        return """
            Extract flashcard pairs (term and definition) from the following text.
            For each card, provide:
            1. term: The key concept or word.
            2. definition: A clear, concise English explanation.
            3. japaneseTranslation: A natural Japanese translation of the definition.
            
            Return ONLY a JSON array of objects with "term", "definition", and "japaneseTranslation" keys.
            IMPORTANT: Ensure the definition is meaningful and specifically describes the term.
            Text: $ocrText
        """.trimIndent()
    }

    fun buildImprovedPrompt(ocrText: String, failedTerm: String): String {
        return """
            Extract flashcard pairs from the following text.
            I noticed the previous attempt for "$failedTerm" was too generic. 
            Please ensure the definition specifically describes "$failedTerm" based on the provided context.
            
            Return ONLY a JSON array of objects with "term", "definition", and "japaneseTranslation" keys.
            Text: $ocrText
        """.trimIndent()
    }
}

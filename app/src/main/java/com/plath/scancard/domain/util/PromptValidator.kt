package com.plath.scancard.domain.util

// IMP-07 7-6: isValid() は ExtractCardsUseCase の3回リトライループ内で all { isValid } として評価（design.md 483-543）。
// generic/insufficient context 検出時は TranslationPromptBuilder.buildImprovedPrompt でリトライ。配線は ExtractCardsUseCase:42 で確認済み。
class PromptValidator {
    fun isValid(term: String, definition: String): Boolean {
        if (definition.isBlank()) return false
        
        val genericPhrases = listOf("a topic to explore", "insufficient context", "no description available")
        if (genericPhrases.any { definition.lowercase().contains(it) }) return false
        
        // Basic check: is it too short?
        if (definition.length < 5) return false
        
        return true
    }
}

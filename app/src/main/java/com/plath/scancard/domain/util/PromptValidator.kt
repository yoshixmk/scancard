package com.plath.scancard.domain.util

// IMP-07 7-6: isValid() is evaluated as "all { isValid }" within ExtractCardsUseCase's 3-retry loop (design.md 483-543).
// Retry with TranslationPromptBuilder.buildImprovedPrompt upon generic/insufficient context detection. Wiring is verified in ExtractCardsUseCase:42.
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

package com.example.scancard.domain.util

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

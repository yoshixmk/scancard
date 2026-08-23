package com.plath.scancard.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptValidatorTest {

    private val validator = PromptValidator()

    @Test
    fun isValid_validDefinition_returnsTrue() {
        assertTrue(validator.isValid("Hello", "A greeting used to say hi"))
        assertTrue(validator.isValid("Dog", "A domestic animal"))
        assertTrue(validator.isValid("Term", "Valid definition with sufficient length"))
    }

    @Test
    fun isValid_blankDefinition_returnsFalse() {
        assertFalse(validator.isValid("Hello", ""))
        assertFalse(validator.isValid("Hello", "   "))
        assertFalse(validator.isValid("Hello", "\n\t"))
    }

    @Test
    fun isValid_tooShort_returnsFalse() {
        assertFalse(validator.isValid("Hello", "ab"))
        assertFalse(validator.isValid("Hello", "1234"))
        assertFalse(validator.isValid("Hello", "Hi"))
        // length < 5
        assertFalse(validator.isValid("Term", "abcd"))
    }

    @Test
    fun isValid_genericDetection_aTopicToExplore_returnsFalse() {
        assertFalse(validator.isValid("Term", "This is a topic to explore in detail"))
        assertFalse(validator.isValid("Term", "A TOPIC TO EXPLORE"))
        assertFalse(validator.isValid("Term", "a topic to explore"))
    }

    @Test
    fun isValid_genericDetection_insufficientContext_returnsFalse() {
        assertFalse(validator.isValid("Term", "insufficient context for definition"))
        assertFalse(validator.isValid("Term", "Insufficient Context"))
        assertFalse(validator.isValid("Term", "INSUFFICIENT CONTEXT"))
    }

    @Test
    fun isValid_genericDetection_noDescriptionAvailable_returnsFalse() {
        assertFalse(validator.isValid("Term", "No description available for this term"))
        assertFalse(validator.isValid("Term", "no description available"))
        assertFalse(validator.isValid("Term", "NO DESCRIPTION AVAILABLE"))
    }

    @Test
    fun isValid_genericDetection_caseInsensitive() {
        // Generic phrase detection should be case-insensitive (implementation uses lowercase)
        assertFalse(validator.isValid("Term", "A Topic To Explore with caps"))
        assertFalse(validator.isValid("Term", "InSuFfIcIeNt CoNtExT"))
    }

    @Test
    fun isValid_genericDetection_partialContains_returnsFalse() {
        assertFalse(validator.isValid("Term", "This definition contains insufficient context somewhere"))
        assertFalse(validator.isValid("Term", "Prefix a topic to explore suffix"))
    }

    @Test
    fun isValid_validWithSimilarButNotGeneric_returnsTrue() {
        assertTrue(validator.isValid("Term", "This is a detailed exploration of the topic"))
        assertTrue(validator.isValid("Term", "Context is sufficient for understanding"))
        assertTrue(validator.isValid("Term", "Description is available here"))
    }

    @Test
    fun isValid_boundaryLength5_returnsTrueIfNotGeneric() {
        assertTrue(validator.isValid("Term", "12345"))
        assertTrue(validator.isValid("Term", "abcde"))
    }
}

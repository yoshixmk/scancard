package com.example.scancard.data.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class CardResponseParserTest {
    private val parser = CardResponseParser()

    @Test
    fun `parse valid JSON array returns correct cards`() {
        val input = """[{"term": "Hello", "definition": "Greeting"}]"""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Hello", result[0].term)
        assertEquals("Greeting", result[0].definition)
    }

    @Test
    fun `parse text with embedded JSON array returns correct cards`() {
        val input = """Here is the data: [{"term": "World", "definition": "Earth"}] and more text."""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("World", result[0].term)
    }

    @Test
    fun `parse invalid format returns empty list`() {
        val input = "Not a json"
        val result = parser.parse(input)
        assertEquals(0, result.size)
    }

    @Test
    fun `parse with japaneseTranslation returns it`() {
        val input = """[{"term": "Dog", "definition": "Animal", "japaneseTranslation": "犬"}]"""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("犬", result[0].japaneseTranslation)
    }
}

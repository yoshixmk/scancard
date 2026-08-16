package com.example.scancard.data.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class CardResponseParserTest {
    private val parser = CardResponseParser()

    @Test
    fun `parse valid JSON array`() {
        val input = """[{"term": "Apple", "definition": "A red fruit"}]"""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Apple", result[0].term)
        assertEquals("A red fruit", result[0].definition)
    }

    @Test
    fun `parse JSON wrapped in text`() {
        val input = """Here is the result: [{"term": "Banana", "definition": "A yellow fruit"}] Hope this helps!"""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Banana", result[0].term)
    }

    @Test
    fun `parse using regex fallback`() {
        val input = """Pair: "term": "Cat", "definition": "An animal"."""
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Cat", result[0].term)
        assertEquals("An animal", result[0].definition)
    }
}

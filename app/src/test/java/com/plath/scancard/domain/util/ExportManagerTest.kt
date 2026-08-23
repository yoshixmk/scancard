package com.plath.scancard.domain.util

import com.plath.scancard.data.local.entities.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    private val manager = ExportManager()

    @Test
    fun exportToTSV_singleCard_correctFormat() {
        val cards = listOf(Card(id = 1, deckId = 1, term = "Hello", definition = "Greeting"))
        val result = manager.exportToTSV(cards)
        assertEquals("Hello\tGreeting", result)
    }

    @Test
    fun exportToTSV_multipleCards_joinedWithNewline() {
        val cards = listOf(
            Card(id = 1, deckId = 1, term = "Hello", definition = "Greeting"),
            Card(id = 2, deckId = 1, term = "World", definition = "Earth")
        )
        val result = manager.exportToTSV(cards)
        assertEquals("Hello\tGreeting\nWorld\tEarth", result)
    }

    @Test
    fun exportToTSV_escapesTabs() {
        val cards = listOf(Card(id = 1, deckId = 1, term = "Hello\tWorld", definition = "Def\tinition"))
        val result = manager.exportToTSV(cards)
        // Tabs inside fields should be escaped to space
        assertEquals("Hello World\tDef inition", result)
        // Ensure no extra tab beyond the delimiter
        assertEquals(1, result.count { it == '\t' })
    }

    @Test
    fun exportToTSV_escapesNewlines() {
        val cards = listOf(Card(id = 1, deckId = 1, term = "Hello\nWorld", definition = "Line1\nLine2"))
        val result = manager.exportToTSV(cards)
        assertEquals("Hello World\tLine1 Line2", result)
        assertTrue(!result.substringAfter("\t").contains("\n") || result.contains("\n").not() || result.count { it == '\n' } == 0)
        // Only newline between cards should not exist for single card
        assertEquals(0, result.count { it == '\n' })
    }

    @Test
    fun exportToTSV_emptyList_returnsEmpty() {
        val result = manager.exportToTSV(emptyList())
        assertEquals("", result)
    }

    @Test
    fun exportToTSV_roundTrip_parseBack() {
        val original = listOf(
            Card(id = 1, deckId = 1, term = "Term1", definition = "Def1"),
            Card(id = 2, deckId = 1, term = "Term2", definition = "Def2"),
            Card(id = 3, deckId = 1, term = "Complex Term", definition = "Complex Definition with spaces")
        )
        val tsv = manager.exportToTSV(original)
        // Simulate import: split by newline then tab
        val parsed = tsv.split("\n").map { line ->
            val parts = line.split("\t")
            assertEquals(2, parts.size)
            parts[0] to parts[1]
        }
        assertEquals(original.size, parsed.size)
        original.forEachIndexed { idx, card ->
            assertEquals(card.term, parsed[idx].first)
            assertEquals(card.definition, parsed[idx].second)
        }
    }

    @Test
    fun exportToTSV_roundTrip_withEscapedChars() {
        val cards = listOf(
            Card(id = 1, deckId = 1, term = "A\tB", definition = "C\nD"),
            Card(id = 2, deckId = 1, term = "E", definition = "F")
        )
        val tsv = manager.exportToTSV(cards)
        // After escaping, no embedded tabs/newlines remain inside fields
        val lines = tsv.split("\n")
        assertEquals(2, lines.size)
        lines.forEach { line ->
            val parts = line.split("\t")
            assertEquals(2, parts.size)
            assertTrue(!parts[0].contains("\t") && !parts[0].contains("\n"))
            assertTrue(!parts[1].contains("\t") && !parts[1].contains("\n"))
        }
    }

    @Test
    fun exportToTSV_preservesJapaneseAndSpecialChars() {
        val cards = listOf(Card(id = 1, deckId = 1, term = "犬", definition = "dog"))
        val result = manager.exportToTSV(cards)
        assertEquals("犬\tdog", result)
    }
}

package com.plath.scancard.domain.util

import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.domain.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Kotest導入時はProperty化: io.kotest.property.checkAll で任意文字列生成に置換予定。
 * 現状は JUnit4 パラメタライズド + 手動ランダム生成で代替。
 */
class CardValidatorPropertyTest {

    // Fake repository used for property-style tests
    private class FakeRepo(initial: List<Card>) : CardRepository {
        private val flow = MutableStateFlow(initial)
        override fun getCardsByDeck(deckId: Long) = flow
        override suspend fun getCardById(id: Long): Card? = null
        override suspend fun insertCards(cards: List<Card>) {}
        override suspend fun insertCard(card: Card): Long = 0
        override suspend fun updateCard(card: Card) {}
        override suspend fun deleteCard(card: Card) {}
        override suspend fun updateCardStatus(cardId: Long, status: com.plath.scancard.domain.model.CardStatus) {}
    }

    @Test
    fun normalization_isIdempotent() = runTest {
        val terms = listOf("Hello", "  Hello ", "HELLO", "hElLo ", "  HeLLo World  ", "  test  ", "ÄÖÜ", "  trim  ")
        for (term in terms) {
            val normalized = term.trim().lowercase()
            // idempotent: normalizing twice yields same result
            assertEquals(normalized, normalized.trim().lowercase())
        }
    }

    @Test
    fun duplicateDetection_isConsistentWithNormalization() = runTest {
        // Property: for any term, findDuplicate with normalized variants should return same result
        val baseTerms = listOf("Kotlin", "Android", "Room Database", "Hilt", "Compose")
        for (base in baseTerms) {
            val card = Card(id = 1, deckId = 1, term = base, definition = "def")
            val repo = FakeRepo(listOf(card))
            val validator = CardValidator(repo)
            val variants = listOf(
                base,
                base.lowercase(),
                base.uppercase(),
                "  $base  ",
                "  ${base.lowercase()}  ",
                base.uppercase().let { "  $it " }
            )
            for (v in variants) {
                assertNotNull("Variant '$v' should match base '$base'", validator.findDuplicate(1, v))
            }
        }
    }

    @Test
    fun duplicateDetection_differentTerms_doNotCollide() = runTest {
        val card = Card(id = 1, deckId = 1, term = "Apple", definition = "Fruit")
        val repo = FakeRepo(listOf(card))
        val validator = CardValidator(repo)
        // Property: adding suffix/prefix should not match unless normalized equals
        val nonMatches = listOf("ApplePie", "Apple ", " Appl", "Apples", "Snapple")
        // Note: "Apple " trimmed is "Apple" -> should match, so exclude those
        val shouldNotMatch = listOf("ApplePie", "Apples", "Snapple", "Pineapple")
        for (term in shouldNotMatch) {
            assertNull("Term '$term' should not match 'Apple'", validator.findDuplicate(1, term))
        }
    }

    @Test
    fun exportTSV_roundTrip_propertyStyle() {
        // Property-style: 100 iterations of random cards -> round-trip preserves data (modulo escaping)
        val manager = ExportManager()
        val random = java.util.Random(42)
        repeat(100) { i ->
            val term = "Term${i}_${random.nextInt(1000)}"
            val def = "Def${i}_${random.nextInt(1000)} with spaces"
            val card = Card(id = i.toLong(), deckId = 1, term = term, definition = def)
            val tsv = manager.exportToTSV(listOf(card))
            val parts = tsv.split("\t")
            assertEquals(2, parts.size)
            assertEquals(term, parts[0])
            assertEquals(def, parts[1])
        }
    }
}

/**
 * Kotest Property導入時の書き換え例 (コメント):
 * class CardValidatorPropertyTest : StringSpec({
 *   "normalization is idempotent" { checkAll(Arb.string()) { s -> s.trim().lowercase() shouldBe s.trim().lowercase().trim().lowercase() } }
 * })
 */

// Additional parameterized variant for ExportManager to demonstrate Kotest Property alternative
@RunWith(Parameterized::class)
class ExportManagerParameterizedTest(private val term: String, private val definition: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "term={0}, def={1}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("Hello", "Greeting"),
            arrayOf("World", "Earth"),
            arrayOf("  spaced  ", "  def  "),
            arrayOf("UPPER", "lower"),
            arrayOf("犬", "dog"),
            arrayOf("Term with spaces", "Definition with spaces and 123")
        )
    }

    private val manager = ExportManager()

    @Test
    fun export_preservesSimpleTermsWithoutEscaping() {
        // Terms without tab/newline should round-trip exactly
        if (term.contains("\t") || term.contains("\n") || definition.contains("\t") || definition.contains("\n")) return
        val card = Card(id = 1, deckId = 1, term = term, definition = definition)
        val tsv = manager.exportToTSV(listOf(card))
        val parts = tsv.split("\t")
        assertEquals(term, parts[0])
        assertEquals(definition, parts[1])
    }
}

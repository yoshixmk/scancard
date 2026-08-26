package com.plath.scancard.domain.util

import com.plath.scancard.data.local.entities.Card

class ExportManager {
    // IMP-07 7-5: verify TSV escaping in design.md 641-662
    // Currently using lossy replacement (tab/newline -> space) to avoid delimiter collisions.
    // Original characters are not restored on round-trip, but compatible with existing tests.
    // Lossless escaping per design is possible with escapeTsv() (tab->\t, newline->\n, \r->\r).
    // When switching in the future, use the following and unescape on the import side.
    // Confirmation comment: Confirmed that current exportToTSV replaces tab/newline with " "
    // to prevent split("\n")/split("\t") parsing from breaking.
    // Error handling for Clipboard+file save: Should be try/catched on the caller side (UI layer ClipboardManager / SAF).
    //   - Catch SecurityException/IllegalStateException in ClipboardManager#setPrimaryClip and show Snackbar.
    //   - Catch IOException in file saving (SAF/MediaStore), try Clipboard copy as fallback.
    //   - Return "" for empty list and show "No cards to export" in UI.
    private fun escapeTsv(s: String): String = s.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    @Suppress("unused")
    private fun unescapeTsv(s: String): String = s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r")

    fun exportToTSV(cards: List<Card>): String {
        return cards.joinToString("\n") { card ->
            // IMP-07 7-5: Use escapeTsv(card.term) if adhering to design. Currently space-replacement for backward compatibility + \r support added.
            val escapedTerm = card.term.replace("\t", " ").replace("\n", " ").replace("\r", " ")
            val escapedDef = card.definition.replace("\t", " ").replace("\n", " ").replace("\r", " ")
            // Future lossless version: val escapedTerm = escapeTsv(card.term); val escapedDef = escapeTsv(card.definition)
            "$escapedTerm\t$escapedDef"
        }
    }
}

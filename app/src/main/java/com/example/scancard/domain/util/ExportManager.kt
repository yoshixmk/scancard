package com.example.scancard.domain.util

import com.example.scancard.data.local.entities.Card

class ExportManager {
    fun exportToTSV(cards: List<Card>): String {
        return cards.joinToString("\n") { card ->
            val escapedTerm = card.term.replace("\t", " ").replace("\n", " ")
            val escapedDef = card.definition.replace("\t", " ").replace("\n", " ")
            "$escapedTerm\t$escapedDef"
        }
    }
}

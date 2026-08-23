package com.plath.scancard.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan/{deckId}") {
        fun createRoute(deckId: Long) = "scan/$deckId"
    }
    object ExtractionPreview : Screen("extraction_preview/{deckId}") {
        fun createRoute(deckId: Long) = "extraction_preview/$deckId"
    }
    object DeckDetail : Screen("deck_detail/{deckId}") {
        fun createRoute(deckId: Long) = "deck_detail/$deckId"
    }
    object Study : Screen("study/{deckId}") {
        fun createRoute(deckId: Long) = "study/$deckId"
    }
    object Export : Screen("export/{deckId}") {
        fun createRoute(deckId: Long) = "export/$deckId"
    }
}

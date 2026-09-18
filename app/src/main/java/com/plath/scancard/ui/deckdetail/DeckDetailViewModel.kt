package com.plath.scancard.ui.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.service.BackgroundTaskManager
import com.plath.scancard.domain.usecase.ManageDeckUseCase
import com.plath.scancard.domain.usecase.StudyCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val manageDeckUseCase: ManageDeckUseCase,
    private val studyCardsUseCase: StudyCardsUseCase,
    private val backgroundTaskManager: BackgroundTaskManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    val deck: StateFlow<Deck?> = manageDeckUseCase.getDecks()
        .map { decks -> decks.find { it.id == deckId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val cards: StateFlow<List<Card>> = studyCardsUseCase.getCards(deckId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateDeckTitle(newTitle: String) {
        viewModelScope.launch {
            deck.value?.let {
                manageDeckUseCase.updateDeck(it.copy(title = newTitle))
            }
        }
    }

    fun addCard(term: String, definition: String) {
        viewModelScope.launch {
            val newCard = Card(
                deckId = deckId,
                term = term,
                definition = definition
            )
            studyCardsUseCase.insertCard(newCard)
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch {
            studyCardsUseCase.updateCard(card)
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch {
            studyCardsUseCase.deleteCard(card)
        }
    }

    /**
     * Manual re-extraction for decks with no cards (e.g. single-prompt LLM
     * returned nothing). Idempotent: exact duplicates are skipped on re-run,
     * and [BackgroundTaskManager.startExtraction] no-ops while work is active.
     */
    fun retryExtraction() {
        viewModelScope.launch {
            backgroundTaskManager.startExtraction(deckId, ModelConfig.DEFAULT_ID)
        }
    }
}

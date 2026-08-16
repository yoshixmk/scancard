package com.example.scancard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scancard.data.local.entities.Deck
import com.example.scancard.domain.usecase.ManageDeckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val manageDeckUseCase: ManageDeckUseCase
) : ViewModel() {

    val decks: StateFlow<List<Deck>> = manageDeckUseCase.getDecks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createDeck(title: String) {
        viewModelScope.launch {
            try {
                manageDeckUseCase.createDeck(title)
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            manageDeckUseCase.deleteDeck(deck)
        }
    }
}

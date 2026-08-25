package com.plath.scancard.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plath.scancard.data.local.entities.Deck
import com.plath.scancard.domain.usecase.ManageDeckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createDeck(title: String) {
        viewModelScope.launch {
            try {
                manageDeckUseCase.createDeck(title)
                _error.value = null
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to create deck", e)
                _error.value = e.message ?: "Failed to create deck"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            manageDeckUseCase.deleteDeck(deck)
        }
    }
}

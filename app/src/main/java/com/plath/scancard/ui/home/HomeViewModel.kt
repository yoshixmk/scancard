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
            val t0 = android.os.SystemClock.elapsedRealtime()
            val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            Log.d("HomeViewModel", "createDeck start wall=$wall title=$title")
            try {
                val id = manageDeckUseCase.createDeck(title)
                val dt = android.os.SystemClock.elapsedRealtime() - t0
                Log.d("HomeViewModel", "createDeck success id=$id took=${dt}ms wall=$wall title=$title")
                _error.value = null
            } catch (e: Exception) {
                val dt = android.os.SystemClock.elapsedRealtime() - t0
                Log.e("HomeViewModel", "Failed to create deck after ${dt}ms wall=$wall title=$title", e)
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

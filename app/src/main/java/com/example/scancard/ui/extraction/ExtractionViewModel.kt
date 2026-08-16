package com.example.scancard.ui.extraction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scancard.domain.usecase.ExtractCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val extractCardsUseCase: ExtractCardsUseCase
) : ViewModel() {

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting = _isExtracting.asStateFlow()

    fun startExtraction(deckId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isExtracting.value = true
            try {
                extractCardsUseCase.extractAndSaveCards(deckId)
                onComplete()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isExtracting.value = false
            }
        }
    }
}

package com.example.scancard.ui.extraction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState
import com.example.scancard.domain.repository.ModelRepository
import com.example.scancard.domain.usecase.ExtractCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val extractCardsUseCase: ExtractCardsUseCase,
    private val modelRepository: ModelRepository
) : ViewModel() {

    val modelState: StateFlow<ModelState> = modelRepository.modelState

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting = _isExtracting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedModel = MutableStateFlow(ModelConfig.GEMMA_4_E2B)
    val selectedModel = _selectedModel.asStateFlow()

    init {
        checkModelStatus()
    }

    fun getAvailableModels() = modelRepository.getAvailableModels()

    fun selectModel(config: ModelConfig) {
        _selectedModel.value = config
        checkModelStatus()
    }

    fun checkModelStatus() {
        modelRepository.checkModelStatus(_selectedModel.value)
    }

    fun downloadModel() {
        viewModelScope.launch {
            _error.value = null
            modelRepository.downloadModel(_selectedModel.value)
        }
    }

    fun startExtraction(deckId: Long, onComplete: () -> Unit) {
        android.util.Log.d("ExtractionVM", "Starting extraction for deck: $deckId")
        viewModelScope.launch {
            _isExtracting.value = true
            _error.value = null
            try {
                extractCardsUseCase.extractAndSaveCards(deckId, _selectedModel.value)
                onComplete()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isExtracting.value = false
            }
        }
    }
}

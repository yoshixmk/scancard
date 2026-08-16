package com.example.scancard.ui.extraction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scancard.data.auth.AuthManager
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
    private val modelRepository: ModelRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val modelState: StateFlow<ModelState> = modelRepository.modelState
    val authState = authManager.authState

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting = _isExtracting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedModel = MutableStateFlow(ModelConfig.GEMMA_4_E2B)
    val selectedModel = _selectedModel.asStateFlow()

    fun getAvailableModels() = modelRepository.getAvailableModels()

    fun selectModel(config: ModelConfig) {
        _selectedModel.value = config
    }

    fun getAuthIntent() = authManager.getAuthIntent()

    fun handleAuthResponse(intent: android.content.Intent) {
        authManager.handleAuthResponse(intent)
    }

    fun downloadModel() {
        viewModelScope.launch {
            _error.value = null
            modelRepository.downloadModel(_selectedModel.value)
        }
    }

    fun startExtraction(deckId: Long, onComplete: () -> Unit) {
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

package com.example.scancard.ui.extraction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState
import com.example.scancard.domain.repository.ModelRepository
import com.example.scancard.domain.service.BackgroundTaskManager
import com.example.scancard.domain.usecase.ExtractCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val backgroundTaskManager: BackgroundTaskManager
) : ViewModel() {

    val modelState: StateFlow<ModelState> = modelRepository.modelState

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedModel = MutableStateFlow(ModelConfig.GEMMA_4_E2B)
    val selectedModel = _selectedModel.asStateFlow()

    private val _deckId = MutableStateFlow<Long?>(null)
    
    val extractionWorkInfo: StateFlow<WorkInfo?> = _deckId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else backgroundTaskManager.getWorkInfo(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isExtracting: StateFlow<Boolean> = extractionWorkInfo.map { 
        it?.state == WorkInfo.State.RUNNING || it?.state == WorkInfo.State.ENQUEUED
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkModelStatus()
    }

    fun setDeckId(id: Long) {
        _deckId.value = id
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

    fun startExtraction(deckId: Long) {
        android.util.Log.d("ExtractionVM", "Starting background extraction for deck: $deckId")
        _deckId.value = deckId
        _error.value = null
        backgroundTaskManager.startExtraction(deckId, _selectedModel.value.id)
    }
}

package com.plath.scancard.ui.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plath.scancard.domain.usecase.ExportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _exportText = MutableStateFlow("")
    val exportText = _exportText.asStateFlow()

    init {
        generatePreview("TSV")
    }

    fun generatePreview(format: String) {
        viewModelScope.launch {
            _exportText.value = if (format == "TSV") {
                exportDataUseCase.exportToTsv(deckId)
            } else {
                exportDataUseCase.exportToCsv(deckId)
            }
        }
    }
}

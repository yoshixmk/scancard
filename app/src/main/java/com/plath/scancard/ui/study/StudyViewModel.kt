package com.plath.scancard.ui.study

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plath.scancard.data.local.entities.Card
import com.plath.scancard.domain.model.CardStatus
import com.plath.scancard.domain.model.FilterType
import com.plath.scancard.domain.usecase.StudyCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val FILTER_TYPE_KEY = stringPreferencesKey("filter_type")

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyCardsUseCase: StudyCardsUseCase,
    private val dataStore: DataStore<Preferences>,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _filter = MutableStateFlow(FilterType.ALL)
    val filter = _filter.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { it[FILTER_TYPE_KEY] }.collect { saved ->
                _filter.value = runCatching { FilterType.valueOf(saved ?: "ALL") }
                    .getOrDefault(FilterType.ALL)
            }
        }
    }

    val cards: StateFlow<List<Card>> = combine(
        studyCardsUseCase.getCards(deckId),
        _filter
    ) { allCards, currentFilter ->
        when (currentFilter) {
            FilterType.ALL -> allCards
            FilterType.NEW -> allCards.filter { it.status == CardStatus.NEW }
            FilterType.LEARNING -> allCards.filter { it.status == CardStatus.LEARNING }
            FilterType.REVIEW -> allCards.filter { it.status == CardStatus.REVIEW }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun nextCard() {
        if (_currentIndex.value < cards.value.size - 1) {
            _currentIndex.value++
        }
    }

    fun previousCard() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
        }
    }

    fun markAsLearned(cardId: Long) {
        viewModelScope.launch {
            studyCardsUseCase.markAsLearned(cardId)
        }
    }

    fun markAsReviewNeeded(cardId: Long) {
        viewModelScope.launch {
            studyCardsUseCase.markAsReviewNeeded(cardId)
        }
    }

    fun setFilter(filter: FilterType) {
        viewModelScope.launch {
            dataStore.edit { it[FILTER_TYPE_KEY] = filter.name }
        }
        _filter.value = filter
        _currentIndex.value = 0
    }
}

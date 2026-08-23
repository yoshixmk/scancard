package com.plath.scancard.ui.study

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// IMP-07 7-4: DataStore<Preferences> filter_type 永続化手順（JVM8制約でコメントのみ、Req9.6対応）
// 手順:
//  1) app/build.gradle.kts に `implementation("androidx.datastore:datastore-preferences:1.1.1")` 追加
//     （依存追加はgradle.properties/build.gradle.ktsのコメント参照）
//  2) @Inject constructor に `val dataStore: DataStore<Preferences>` をHiltで注入（PreferencesDataStore delegate）
//  3) init で復元: viewModelScope.launch { dataStore.data.map { it[stringPreferencesKey("filter_type")] }.collect { saved -> _filter.value = runCatching { FilterType.valueOf(saved ?: "ALL") }.getOrDefault(FilterType.ALL) } }
//  4) setFilter() で保存: viewModelScope.launch { dataStore.edit { it[stringPreferencesKey("filter_type")] = filter.name } }
// 現状はメモリ保持のみ。TODO(IMP-07): DataStore<Preferences> filter_type で永続化（下記 setFilter/initのTODO参照）。
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyCardsUseCase: StudyCardsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    // TODO(IMP-07): DataStore<Preferences> filter_type で永続化
    // init で dataStore.data.collect して _filter を復元すること（手順はクラス冒頭コメント参照）
    private val _filter = MutableStateFlow(FilterType.ALL)
    val filter = _filter.asStateFlow()

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
        // TODO(IMP-07): DataStore<Preferences> filter_type で永続化
        // viewModelScope.launch { dataStore.edit { it[stringPreferencesKey("filter_type")] = filter.name } }
        // 手順詳細はクラス冒頭コメント参照。依存追加は app/build.gradle.kts コメント参照。
        _filter.value = filter
        _currentIndex.value = 0
    }
}

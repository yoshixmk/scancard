package com.plath.scancard.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.data.local.entities.Card

import com.plath.scancard.domain.model.FilterType
import com.plath.scancard.domain.model.LanguagePreference
// TODO(IMP-05 5-6/5-7): Adaptive FlashCard 用 import（コメント留め — 有効化時にアンコメント）
// import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
// import androidx.window.core.layout.WindowSizeClass
// import com.plath.scancard.ui.preview.FormFactorPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val filter by viewModel.filter.collectAsState()
    
    var languagePreference by remember { mutableStateOf(LanguagePreference.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilterMenu(currentFilter = filter, onFilterSelected = viewModel::setFilter)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cards to study in this filter.")
                }
            } else {
                val currentCard = cards.getOrNull(currentIndex) ?: return@Column
                
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / cards.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                Text(
                    text = "Card ${currentIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .draggable(
                            state = rememberDraggableState { /* ignore deltas */ },
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                if (velocity > 300f) {
                                    viewModel.previousCard()
                                } else if (velocity < -300f) {
                                    viewModel.nextCard()
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Key the flashcard by its ID so rotation state resets on card change
                    key(currentCard.id) {
                        Flashcard(
                            card = currentCard,
                            languagePreference = languagePreference,
                            onToggleLanguage = {
                                languagePreference = if (languagePreference == LanguagePreference.ENGLISH)
                                    LanguagePreference.JAPANESE
                                else
                                    LanguagePreference.ENGLISH
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { 
                            viewModel.markAsReviewNeeded(currentCard.id)
                            viewModel.nextCard()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Need Review")
                    }
                    Button(
                        onClick = { 
                            viewModel.markAsLearned(currentCard.id)
                            viewModel.nextCard()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Learned")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::previousCard, enabled = currentIndex > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }
                    IconButton(onClick = viewModel::nextCard, enabled = currentIndex < cards.size - 1) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun Flashcard(
    card: Card,
    languagePreference: LanguagePreference,
    onToggleLanguage: () -> Unit
) {
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(500),
        label = "cardRotation"
    )

    // TODO(IMP-05 5-6): FlashCard aspectRatio を currentWindowAdaptiveInfo().windowSizeClass で可変にする
    // 現状は固定 0.7f（Phone縦向け）。adaptive 依存有効化後に下記へ置換（ビルドを壊さないためコメント留め）:
    // ```
    // val adaptiveInfo = currentWindowAdaptiveInfo()
    // val aspectRatio = when {
    //     adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 1.6f // expanded: Tablet/Desktop 横長
    //     adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)   -> 1.2f // medium: Foldable unfolded / large phone
    //     else -> 0.7f // compact: Phone
    // }
    // ```
    // 適用: Modifier.aspectRatio(aspectRatio) に置換。MediaQuery代替として WindowSizeClass を使用
    // （adaptive/SKILL.md Step5 MediaQuery 章 — compose.material3.adaptive は MediaQuery 相当）
    // 検証: @FormFactorPreviews (400/700/900/1200dp) でカード縦横比が崩れないことを目視確認
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(0.7f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            }
            .clickable { rotated = !rotated },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                Text(
                    text = card.term,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (languagePreference == LanguagePreference.ENGLISH)
                            card.definition
                        else
                            card.japaneseTranslation.ifBlank { "No translation available" },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    TextButton(onClick = { 
                        // Stop propagation of click to the card
                        onToggleLanguage() 
                    }) {
                        Text(if (languagePreference == LanguagePreference.ENGLISH) "日本語" else "English")
                    }
                }
            }
        }
    }
}

// TODO(IMP-05 5-7): @FormFactorPreviews 適用手順
// ```
// @FormFactorPreviews
// @Composable
// fun StudyScreenPreview() { MaterialTheme { StudyScreen(onBack={}) } }
// @FormFactorPreviews
// @Composable
// fun FlashcardPreview() { MaterialTheme { Flashcard(card=Card(...), languagePreference=ENGLISH, onToggleLanguage={}) } }
// ```

@Composable
fun FilterMenu(currentFilter: FilterType, onFilterSelected: (FilterType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Filter: ${currentFilter.name}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FilterType.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.name) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

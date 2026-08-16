package com.example.scancard.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.example.scancard.data.local.entities.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                val currentCard = cards[currentIndex]
                
                LinearProgressIndicator(
                    progress = (currentIndex + 1).toFloat() / cards.size,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                Text(
                    text = "Card ${currentIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(32.dp))

                Flashcard(card = currentCard)

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.markAsReviewNeeded(currentCard.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Text("Need Review")
                    }
                    Button(
                        onClick = { viewModel.markAsLearned(currentCard.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Learned")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::previousCard, enabled = currentIndex > 0) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                    }
                    IconButton(onClick = viewModel::nextCard, enabled = currentIndex < cards.size - 1) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun Flashcard(card: Card) {
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(500)
    )

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
                Text(
                    text = card.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Composable
fun FilterMenu(currentFilter: StudyFilter, onFilterSelected: (StudyFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Filter: ${currentFilter.name}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StudyFilter.values().forEach { filter ->
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

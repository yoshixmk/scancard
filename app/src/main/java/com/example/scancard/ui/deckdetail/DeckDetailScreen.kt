package com.example.scancard.ui.deckdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scancard.data.local.entities.Card as FlashCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    onBack: () -> Unit,
    onStudyClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel()
) {
    val deck by viewModel.deck.collectAsState()
    val cards by viewModel.cards.collectAsState()
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isEditingTitle) {
                        TextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            singleLine = true
                        )
                    } else {
                        Text(deck?.title ?: "Loading...") 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditingTitle) {
                        TextButton(onClick = { 
                            viewModel.updateDeckTitle(editedTitle)
                            isEditingTitle = false 
                        }) {
                            Text("Save")
                        }
                    } else {
                        IconButton(onClick = { 
                            editedTitle = deck?.title ?: ""
                            isEditingTitle = true 
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Title")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { deck?.let { onStudyClick(it.id) } }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Study")
                }
                OutlinedButton(onClick = { deck?.let { onExportClick(it.id) } }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export")
                }
            }

            Text(
                text = "${cards.size} Cards",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge
            )

            LazyColumn {
                items(cards) { card ->
                    CardListItem(card)
                }
            }
        }
    }
}

@Composable
fun CardListItem(card: FlashCard) {
    ListItem(
        headlineContent = { Text(card.term) },
        supportingContent = { Text(card.definition) },
        trailingContent = {
            if (card.isLearned) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Learned", modifier = Modifier.padding(4.dp))
                }
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("New", modifier = Modifier.padding(4.dp))
                }
            }
        }
    )
}

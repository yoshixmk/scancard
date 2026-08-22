package com.example.scancard.ui.deckdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
    onAddByScanClick: (Long) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel()
) {
    val deck by viewModel.deck.collectAsState()
    val cards by viewModel.cards.collectAsState()
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    
    var cardToEdit by remember { mutableStateOf<FlashCard?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
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
                text = "${cards.size} Cards (ID: ${deck?.id ?: "N/A"})",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge
            )

            LazyColumn {
                items(cards) { card ->
                    CardListItem(
                        card = card,
                        onClick = { cardToEdit = card },
                        onDelete = { viewModel.deleteCard(card) }
                    )
                }
            }
        }
    }

    if (cardToEdit != null) {
        CardEditDialog(
            card = cardToEdit!!,
            onDismiss = { cardToEdit = null },
            onSave = { updatedCard ->
                viewModel.updateCard(updatedCard)
                cardToEdit = null
            }
        )
    }

    if (showAddDialog) {
        AddCardOptionsDialog(
            onDismiss = { showAddDialog = false },
            onManualAdd = { term, definition ->
                viewModel.addCard(term, definition)
                showAddDialog = false
            },
            onScanAdd = {
                deck?.let { onAddByScanClick(it.id) }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CardListItem(
    card: FlashCard,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(card.term) },
        supportingContent = { Text(card.definition) },
        trailingContent = {
            Row {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}

@Composable
fun CardEditDialog(
    card: FlashCard,
    onDismiss: () -> Unit,
    onSave: (FlashCard) -> Unit
) {
    var term by remember { mutableStateOf(card.term) }
    var definition by remember { mutableStateOf(card.definition) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column {
                TextField(value = term, onValueChange = { term = it }, label = { Text("Term") })
                Spacer(Modifier.height(8.dp))
                TextField(value = definition, onValueChange = { definition = it }, label = { Text("Definition") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(card.copy(term = term, definition = definition)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCardOptionsDialog(
    onDismiss: () -> Unit,
    onManualAdd: (String, String) -> Unit,
    onScanAdd: () -> Unit
) {
    var showManualAdd by remember { mutableStateOf(false) }

    if (showManualAdd) {
        var term by remember { mutableStateOf("") }
        var definition by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("New Card") },
            text = {
                Column {
                    TextField(value = term, onValueChange = { term = it }, label = { Text("Term") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = definition, onValueChange = { definition = it }, label = { Text("Definition") })
                }
            },
            confirmButton = {
                TextButton(onClick = { onManualAdd(term, definition) }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAdd = false }) { Text("Back") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Card") },
            text = { Text("How would you like to add a new card?") },
            confirmButton = {
                TextButton(onClick = onScanAdd) { Text("Scan Document") }
            },
            dismissButton = {
                TextButton(onClick = { showManualAdd = true }) { Text("Manual Entry") }
            }
        )
    }
}

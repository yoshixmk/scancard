package com.plath.scancard.ui.deckdetail

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.data.local.entities.Card as FlashCard
// TODO(IMP-05 5-5): Imports for StaggeredGrid / Grid Adaptive proposal (commented out — uncomment when enabling)
// import androidx.compose.foundation.lazy.grid.GridCells
// import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// import androidx.compose.foundation.lazy.grid.items
// import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
// import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
// import com.plath.scancard.ui.preview.FormFactorPreviews

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
        contentWindowInsets = WindowInsets.safeDrawing,
        // Edge-to-Edge: For verifying StatusBar scrim/List chapter — TopAppBar automatically handles safeDrawing
        topBar = {
            TopAppBar(
                title = { 
                    if (isEditingTitle) {
                        TextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            singleLine = true,
                            modifier = Modifier.imePadding()
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("deckDetailFabAddCard")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { deck?.let { onStudyClick(it.id) } },
                    modifier = Modifier.testTag("deckDetailStudyBtn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Study")
                }
                OutlinedButton(
                    onClick = { deck?.let { onExportClick(it.id) } },
                    modifier = Modifier.testTag("deckDetailExportBtn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export")
                }
            }

            Text(
                text = "${cards.size} Cards (ID: ${deck?.id ?: "N/A"})",
                modifier = Modifier.padding(horizontal = 16.dp).testTag("deckDetailCardCount"),
                style = MaterialTheme.typography.labelLarge
            )

            if (cards.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No cards yet — extraction may have produced nothing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.retryExtraction() },
                        modifier = Modifier.testTag("deckDetailRetryExtractionBtn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry extraction")
                    }
                }
            }

            // TODO(IMP-05 5-5): Proposal to replace LazyColumn with Adaptive Grid/StaggeredGrid (commented out for staged application)
            // Currently remains as LazyColumn to maintain IMP-03 contentPadding. Candidate for
            // multi-column display when card count is high or on tablet widths — commented out to avoid breaking the build.
            // Option A: LazyVerticalGrid (for uniform card heights)
            // ```
            // LazyVerticalGrid(
            //     columns = GridCells.Adaptive(180.dp),
            //     modifier = Modifier.fillMaxSize(),
            //     contentPadding = padding,
            //     verticalArrangement = Arrangement.spacedBy(8.dp),
            //     horizontalArrangement = Arrangement.spacedBy(8.dp)
            // ) {
            //     items(cards) { card ->
            //         CardListItem(card = card, onClick = { cardToEdit = card }, onDelete = { viewModel.deleteCard(card) })
            //     }
            // }
            // ```
            // Option B: LazyVerticalStaggeredGrid (for non-uniform card heights)
            // ```
            // LazyVerticalStaggeredGrid(
            //     columns = StaggeredGridCells.Adaptive(180.dp),
            //     modifier = Modifier.fillMaxSize(),
            //     contentPadding = padding,
            //     verticalItemSpacing = 8.dp,
            //     horizontalArrangement = Arrangement.spacedBy(8.dp)
            // ) {
            //     items(cards) { card ->
            //         CardListItem(card = card, onClick = { cardToEdit = card }, onDelete = { viewModel.deleteCard(card) })
            //     }
            // }
            // ```
            // Evaluation: Option A is stable for uniform ListItem heights; Option B follows height in decks with non-uniform definitions.
            // Visually verify variable column counts in both with FormFactorPreviews (400/700/900/1200dp).
            // SKILL.md Lists chapter: Since parent Column is already inset with padding(padding), limit contentPadding to bottom only to avoid FAB overlap.
            LazyColumn(
                modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 80.dp)
            ) {
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

// TODO(IMP-05 5-7): @FormFactorPreviews application procedure
// ```
// @FormFactorPreviews
// @Composable
// fun DeckDetailScreenPreview() { MaterialTheme { DeckDetailScreen(onBack={}, onStudyClick={}, onExportClick={}, onAddByScanClick={}) } }
// ```

@Composable
fun CardListItem(
    card: FlashCard,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.testTag("cardItem_${card.id}").clickable(onClick = onClick),
        headlineContent = { Text(card.term, modifier = Modifier.testTag("cardTerm_${card.id}")) },
        supportingContent = {
            Column {
                Text(card.definition)
                if (card.status.name != "NEW") {
                    AssistChip(
                        onClick = {},
                        label = { Text(card.status.name) },
                        modifier = Modifier.testTag("cardStatus_${card.id}_${card.status.name}")
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag("cardStatusLabel_${card.id}")
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("cardDelete_${card.id}")
                ) {
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
            // SKILL.md IME chapter PREFERRED: Since safeDrawing is consumed by parent rather than fitInside, limit child's imePadding to TextField only (to avoid double padding).
            Column {
                TextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Term") },
                    modifier = Modifier.imePadding()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition") },
                    modifier = Modifier.imePadding()
                )
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
                    TextField(
                        value = term,
                        onValueChange = { term = it },
                        label = { Text("Term") },
                        modifier = Modifier.imePadding()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = definition,
                        onValueChange = { definition = it },
                        label = { Text("Definition") },
                        modifier = Modifier.imePadding()
                    )
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
                TextButton(
                    onClick = onScanAdd,
                    modifier = Modifier.testTag("addCardScanOption")
                ) { Text("Scan Document") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualAdd = true },
                    modifier = Modifier.testTag("addCardManualOption")
                ) { Text("Manual Entry") }
            }
        )
    }
}

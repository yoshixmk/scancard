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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.data.local.entities.Card as FlashCard
// TODO(IMP-05 5-5): StaggeredGrid / Grid Adaptive 案用 import（コメント留め — 有効化時にアンコメント）
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
        // Edge-to-Edge: StatusBar scrim/List章 検証用 — TopAppBarは自動でsafeDrawingを処理
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
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

            // TODO(IMP-05 5-5): LazyColumn → Adaptive Grid/StaggeredGrid 置換案（コメント留め、段階的適用）
            // 現状は IMP-03 contentPadding 維持のため LazyColumn のまま。カード数多時やTablet幅で
            // 複列表示が必要な場合の候補を以下に記載 — ビルドを壊さないためコメント留め。
            // 案A: LazyVerticalGrid（均等カード高さ向け）
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
            // 案B: LazyVerticalStaggeredGrid（カード高さ不均一な場合）
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
            // 判定: 案Aは ListItem 高さ均一で見栄え安定、案Bは定義長が不均一な deck で高さ追従。
            // いずれも FormFactorPreviews (400/700/900/1200dp) で列数可変を目視検証すること。
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
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

// TODO(IMP-05 5-7): @FormFactorPreviews 適用手順
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
            Column(modifier = Modifier.imePadding()) {
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
                Column(modifier = Modifier.imePadding()) {
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
                TextButton(onClick = onScanAdd) { Text("Scan Document") }
            },
            dismissButton = {
                TextButton(onClick = { showManualAdd = true }) { Text("Manual Entry") }
            }
        )
    }
}

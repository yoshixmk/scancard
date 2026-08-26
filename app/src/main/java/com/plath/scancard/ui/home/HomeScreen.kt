package com.plath.scancard.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.data.local.entities.Deck
// TODO(IMP-05): Imports for Adaptive Grid migration (commented out — uncomment when enabling)
// import androidx.compose.foundation.lazy.grid.GridCells
// import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// import androidx.compose.foundation.lazy.grid.items
// import com.plath.scancard.ui.preview.FormFactorPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onDeckClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val decks by viewModel.decks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newDeckTitle by remember { mutableStateOf("") }

    Scaffold(
        // Edge-to-Edge SKILL.md Step2-3: Scaffold contentWindowInsets=safeDrawing handles systemBars.
        // Adaptive: NavigationSuiteScaffold does not propagate PaddingValues, so individual screens need to handle insets.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text("ScanCard") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                modifier = Modifier.testTag("homeFabScan")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp).fillMaxWidth().testTag("homeCreateDeckBtn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Manual Deck")
            }

            if (decks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No decks yet. Tap camera to start scanning.")
                }
            } else {
                // TODO(IMP-05): Proposal for adaptive replacement of LazyColumn with LazyVerticalGrid
                // Currently remains as LazyColumn to maintain contentPadding from IMP-03. To automatically
                // vary the number of columns for Foldable/Tablet, replace with the following
                // (commented out for staged application and to avoid breaking the build):
                // ```
                // LazyVerticalGrid(
                //     columns = GridCells.Adaptive(320.dp),
                //     modifier = Modifier.weight(1f),
                //     contentPadding = padding, // Maintains IMP-03 edge-to-edge support
                //     verticalArrangement = Arrangement.spacedBy(0.dp),
                //     horizontalArrangement = Arrangement.spacedBy(0.dp)
                // ) {
                //     items(decks) { deck ->
                //         DeckItem(deck = deck, onClick = { onDeckClick(deck.id) }, onDelete = { viewModel.deleteDeck(deck) })
                //     }
                // }
                // ```
                // Activation procedure: Uncomment top imports and replace this LazyColumn block with the above.
                // Existing Preview (HomeScreenPreview) should be verified across 4 form factors using @FormFactorPreviews.
                // SKILL.md Lists chapter: Pass innerPadding to contentPadding to keep top/bottom away from systemBars.
                // Since the parent Column is already inset with Modifier.padding(padding), to avoid double
                // insetting at the top, delegate only bottom to contentPadding and add margin for FAB.
                LazyColumn(
                    modifier = Modifier.weight(1f).consumeWindowInsets(padding),
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 80.dp)
                ) {
                    items(decks) { deck ->
                        DeckItem(
                            deck = deck,
                            onClick = { onDeckClick(deck.id) },
                            onDelete = { viewModel.deleteDeck(deck) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
            AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Deck") },
            text = {
                val isError = newDeckTitle.length > 100
                TextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    label = { Text("Title") },
                    isError = isError,
                    supportingText = {
                        if (isError) Text("Title must be 1-100 characters")
                    },
                    modifier = Modifier.imePadding().testTag("newDeckTitleInput")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createDeck(newDeckTitle)
                        newDeckTitle = ""
                        showAddDialog = false
                    },
                    enabled = newDeckTitle.isNotBlank() && newDeckTitle.length <= 100,
                    modifier = Modifier.testTag("newDeckCreateBtn")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// TODO(IMP-05 5-7): Apply @FormFactorPreviews to each Screen Preview
// Procedure: Enable the preview template below to simultaneously verify 4 form factors (phone/foldable/tablet/desktop)
// ```
// @FormFactorPreviews
// @Composable
// fun HomeScreenPreview() {
//     MaterialTheme { HomeScreen(onScanClick = {}, onDeckClick = {}) }
// }
// @FormFactorPreviews
// @Composable
// fun DeckItemPreview() {
//     MaterialTheme { DeckItem(deck = Deck(id=1, title="Sample", createdAt=Date()), onClick={}, onDelete={}) }
// }
// ```
// Note: Adheres to adaptive skill Step 1 — FormFactorPreviews is defined in ui.preview.FormFactorPreviews.kt

// IMP-09 Styles API experimental - see .kiro/skills/styles

@Composable
fun DeckItem(
    deck: Deck,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("deckCard_${deck.title}")
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = deck.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Created: ${java.text.SimpleDateFormat("yyyy-MM-dd").format(deck.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("deckDeleteBtn_${deck.title}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

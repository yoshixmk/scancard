package com.example.scancard.ui.home

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scancard.data.local.entities.Deck

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
        topBar = {
            TopAppBar(title = { Text("ScanCard") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Document")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp).fillMaxWidth()
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
                LazyColumn(modifier = Modifier.weight(1f)) {
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
                TextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createDeck(newDeckTitle)
                        newDeckTitle = ""
                        showAddDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

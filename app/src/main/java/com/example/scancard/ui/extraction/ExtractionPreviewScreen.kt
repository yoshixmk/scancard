package com.example.scancard.ui.extraction

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionPreviewScreen(
    onBack: () -> Unit,
    onFinish: (Long) -> Unit,
    deckId: Long,
    viewModel: ExtractionViewModel = hiltViewModel()
) {
    val isExtracting by viewModel.isExtracting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extracting Cards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isExtracting) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Gemma is extracting flashcards...")
            } else {
                Text("Text recognition complete.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.startExtraction(deckId) { onFinish(deckId) } }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start AI Extraction")
                }
            }
        }
    }
}

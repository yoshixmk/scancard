package com.example.scancard.ui.extraction

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionPreviewScreen(
    onBack: () -> Unit,
    onFinish: (Long) -> Unit,
    deckId: Long,
    viewModel: ExtractionViewModel = hiltViewModel()
) {
    val isExtracting by viewModel.isExtracting.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val hfToken by viewModel.hfToken.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    LaunchedEffect(deckId) {
        android.util.Log.d("ExtractionPreview", "Screen loaded with deckId: $deckId")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Extraction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        tokenInput = hfToken ?: ""
                        showTokenDialog = true 
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("Select AI Model", style = MaterialTheme.typography.titleLarge)
            
            ModelSelector(
                selectedModel = selectedModel,
                availableModels = viewModel.getAvailableModels(),
                onModelSelected = { viewModel.selectModel(it) },
                enabled = modelState !is ModelState.Downloading && !isExtracting
            )

            HorizontalDivider()

            when (val state = modelState) {
                is ModelState.Idle -> {
                    Text("Model Status: Not Installed", color = MaterialTheme.colorScheme.secondary)
                    
                    if (hfToken.isNullOrBlank()) {
                        Button(onClick = { showTokenDialog = true }) {
                            Text("Setup Hugging Face Token")
                        }
                        Text("A Personal Access Token is required to download gated models.", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Button(onClick = { viewModel.downloadModel() }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download Selected Model")
                        }
                    }
                }
                is ModelState.Downloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(progress = { state.progress })
                        Text("Downloading ${selectedModel.name}... ${(state.progress * 100).toInt()}%")
                    }
                }
                is ModelState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.downloadModel() }) {
                        Text("Retry Download")
                    }
                }
                is ModelState.Ready -> {
                    if (isExtracting) {
                        CircularProgressIndicator()
                        Text("Gemma is extracting flashcards...")
                    } else if (error != null) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.startExtraction(deckId) { onFinish(deckId) } }) {
                            Text("Retry Extraction")
                        }
                    } else {
                        Text("Model Ready: ${selectedModel.name}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { viewModel.startExtraction(deckId) { onFinish(deckId) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start AI Extraction")
                        }
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Hugging Face Settings") },
            text = {
                Column {
                    Text("Enter your Personal Access Token (PAT) from Hugging Face settings.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Access Token") },
                        placeholder = { Text("hf_...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveToken(tokenInput)
                    showTokenDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ModelSelector(
    selectedModel: ModelConfig,
    availableModels: List<ModelConfig>,
    onModelSelected: (ModelConfig) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedCard(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedModel.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(selectedModel.description, style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${selectedModel.sizeGb} GB", style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.ExpandMore, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            availableModels.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.name, fontWeight = FontWeight.Bold)
                            Text(model.description, style = MaterialTheme.typography.bodySmall)
                            Text("Size: ${model.sizeGb} GB", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

package com.example.scancard.ui.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val exportText by viewModel.exportText.collectAsState()
    var selectedFormat by remember { mutableStateOf("TSV") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Deck") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = selectedFormat == "TSV",
                    onClick = { 
                        selectedFormat = "TSV"
                        viewModel.generatePreview("TSV")
                    },
                    label = { Text("TSV (Quizlet)") }
                )
                FilterChip(
                    selected = selectedFormat == "CSV",
                    onClick = { 
                        selectedFormat = "CSV"
                        viewModel.generatePreview("CSV")
                    },
                    label = { Text("CSV") }
                )
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(text = exportText, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ScanCard Export", exportText)
                    clipboard.setPrimaryClip(clip)
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
                
                Button(onClick = {
                    val intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, exportText)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Export"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

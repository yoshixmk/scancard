package com.example.scancard.ui.scan

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onComplete: (Long) -> Unit,
    deckId: Long,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scannedPages by viewModel.scannedPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setPageLimit(100)
        .build()
    
    val scanner = remember { GmsDocumentScanning.getClient(options) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.map { it.imageUri }?.let { uris ->
                viewModel.addPages(uris)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedPages.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.processScans(deckId) { onComplete(deckId) } 
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Done")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (scannedPages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        scanner.getStartScanIntent(context as Activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                    }) {
                        Text("Start Scanning")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(scannedPages) { uri ->
                        Box(modifier = Modifier.padding(4.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.aspectRatio(0.7f),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removePage(uri) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                scanner.getStartScanIntent(context as Activity)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                            },
                            modifier = Modifier.padding(4.dp).aspectRatio(0.7f)
                        ) {
                            Text("Add More")
                        }
                    }
                }
            }
        }
    }
}

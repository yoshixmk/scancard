package com.plath.scancard.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onComplete: (deckId: Long, fastMode: Boolean) -> Unit,
    deckId: Long,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scannedPages by viewModel.scannedPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

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

    // Manual test: Reduced by 2 taps — Automatically launch DocumentScanner after transitioning to ScanScreen (becomes 1 tap)
    // Immediately launch scanner after transitioning from Home FAB / DeckDetail ScanAdd, saving the effort of pressing the conventional "Start Scanning" button.
    // Re-launch suppression: Auto-launch only for the first time with alreadyAutoLaunched; can be re-launched via button on cancellation.
    var alreadyAutoLaunched by remember { mutableStateOf(false) }
    fun launchScanner() {
        val activity = context.findActivity() ?: return
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ScanScreen", "getStartScanIntent failed", e)
            }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-launch after permission is granted (first time only, when scannedPages is empty = new scan)
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !alreadyAutoLaunched && scannedPages.isEmpty() && !isProcessing) {
            alreadyAutoLaunched = true
            launchScanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Edge-to-Edge: For verifying StatusBar scrim/List chapter — TopAppBar automatically handles safeDrawing
            TopAppBar(
                title = { Text("Scan Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedPages.isNotEmpty() && !isProcessing) {
                        TextButton(
                            onClick = {
                                android.util.Log.d("ScanScreen", "Top Bar Done clicked")
                                viewModel.processScans(deckId) { newDeckId, fastMode -> onComplete(newDeckId, fastMode) }
                            },
                            modifier = Modifier.testTag("scanDoneBtn")
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (scannedPages.isNotEmpty() && !isProcessing) {
                // SKILL.md Step3: custom bottomBar is not automatically handled by insets, so explicitly apply safeDrawing bottom
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                ) {
                    Button(
                        onClick = {
                            android.util.Log.d("ScanScreen", "Bottom Finish button clicked")
                            viewModel.processScans(deckId) { newDeckId, fastMode -> onComplete(newDeckId, fastMode) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                            .testTag("scanExtractBtn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Extract Cards (${scannedPages.size})")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
        ) {
            if (!hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera permission is required to scan.")
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (isProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Processing images and OCR...")
                    }
                }
            } else if (scannedPages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Keep manual button as fallback after auto-launch cancel/failure
                        Button(
                            onClick = { launchScanner() },
                            modifier = Modifier.testTag("scanStartBtn")
                        ) {
                            Text("Start Scanning")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Scanner will open automatically", style = MaterialTheme.typography.labelSmall)
                        if (com.plath.scancard.BuildConfig.DEBUG) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.insertDummyScanForE2E(deckId) { newDeckId, fastMode -> onComplete(newDeckId, fastMode) } },
                                modifier = Modifier.testTag("scanDummyInsertBtn")
                            ) {
                                Text("Insert Dummy Scan (E2E)")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    // SKILL.md Lists chapter: Include bottom of Scaffold safeDrawing in contentPadding to avoid bottomBar overlap
                    contentPadding = PaddingValues(
                        start = 8.dp, top = 8.dp, end = 8.dp,
                        bottom = 8.dp + padding.calculateBottomPadding() + 80.dp
                    ),
                    modifier = Modifier.fillMaxSize()
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
                        OutlinedButton(
                            onClick = { launchScanner() },
                            modifier = Modifier.padding(4.dp).aspectRatio(0.7f).testTag("scanAddMoreBtn")
                        ) {
                            Text("Add More")
                        }
                    }
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        item {
                            OutlinedButton(
                                onClick = { viewModel.insertDummyScanForE2E(deckId) { newDeckId, fastMode -> onComplete(newDeckId, fastMode) } },
                                modifier = Modifier.padding(4.dp).aspectRatio(0.7f).testTag("scanDummyInsertBtn")
                            ) {
                                Text("Insert Dummy Scan (E2E)")
                            }
                        }
                    }
                }
            }
        }
    }
}

// TODO(IMP-05 5-7): @FormFactorPreviews application procedure
// ```
// @FormFactorPreviews
// @Composable
// fun ScanScreenPreview() { MaterialTheme { ScanScreen(onBack={}, onComplete={}, deckId=0) } }
// ```
// Already applied LazyVerticalGrid(GridCells.Adaptive(120.dp)) — reference implementation of IMP-05 Step 4.
// Verify 120dp column variation on Tablet/Desktop with FormFactorPreviews.

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

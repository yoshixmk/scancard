package com.plath.scancard.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    var scannerError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Use rememberSaveable to survive config change (rotation) — without it, recreation would auto-launch again while previous scanner overlay is still dimming, appearing as black screen.
    var alreadyAutoLaunched by rememberSaveable { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val uris = scanResult?.pages?.map { it.imageUri }
                if (!uris.isNullOrEmpty()) {
                    scannerError = null
                    viewModel.addPages(uris)
                    android.util.Log.d("ScanScreen", "Scanner returned ${uris.size} pages")
                } else {
                    scannerError = "No pages returned from scanner"
                    android.util.Log.w("ScanScreen", "Scanner OK but no pages: $scanResult")
                }
            }
            Activity.RESULT_CANCELED -> {
                android.util.Log.d("ScanScreen", "Scanner canceled")
                scannerError = "Scan canceled"
            }
            else -> {
                scannerError = "Scanner failed (code=${result.resultCode})"
                android.util.Log.w("ScanScreen", "Scanner failed with code ${result.resultCode}")
            }
        }
    }

    // Manual test: Reduced by 2 taps — Automatically launch DocumentScanner after transitioning to ScanScreen (becomes 1 tap)
    // Immediately launch scanner after transitioning from Home FAB / DeckDetail ScanAdd, saving the effort of pressing the conventional "Start Scanning" button.
    // Re-launch suppression: Auto-launch only for the first time with alreadyAutoLaunched; can be re-launched via button on cancellation.
    fun launchScanner() {
        scannerError = null
        val activity = context.findActivity()
        if (activity == null) {
            scannerError = "Activity not found"
            return
        }
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                try {
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } catch (e: Exception) {
                    scannerError = "Failed to launch scanner: ${e.message}"
                    android.util.Log.e("ScanScreen", "launch failed", e)
                }
            }
            .addOnFailureListener { e ->
                scannerError = "Scanner unavailable: ${e.message} — use gallery or retry"
                android.util.Log.e("ScanScreen", "getStartScanIntent failed", e)
            }
    }

    // Show scanner errors via snackbar (prevents silent black screen)
    LaunchedEffect(scannerError) {
        scannerError?.let { snackbarHostState.showSnackbar(it) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
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
                .background(MaterialTheme.colorScheme.background)
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
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        if (scannerError != null) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(scannerError ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { scannerError = null; launchScanner() }, modifier = Modifier.testTag("scanRetryBtn")) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Retry Scanner")
                                    }
                                }
                            }
                        }
                        // Req23: Direct camera launch — show loading, not primary Start Scanning
                        if (scannerError == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.testTag("scanOpeningIndicator"))
                                Spacer(Modifier.height(12.dp))
                                Text("Opening camera...", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text("Scanner will open automatically", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            }
                        } else {
                            // Fallback only when error/canceled
                            Button(
                                onClick = { scannerError = null; launchScanner() },
                                modifier = Modifier.testTag("scanStartBtn")
                            ) {
                                Text("Start Scanning")
                            }
                        }
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
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    items(scannedPages) { uri ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(0.7f)
                                    .testTag("scanThumb_${uri}"),
                                contentScale = ContentScale.Crop,
                                onError = { android.util.Log.w("ScanScreen", "AsyncImage load failed for $uri: ${it.result.throwable}") }
                            )
                            // Fallback label when image fails — prevents perceived black screen
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Page", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
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

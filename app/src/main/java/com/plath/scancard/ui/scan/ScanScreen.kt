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
    onComplete: (Long) -> Unit,
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

    // Manual test: 2タップ削減 — ScanScreen遷移後に自動でDocumentScannerを起動（1タップ化）
    // Home FAB / DeckDetail ScanAdd からの遷移直後に scanner を即時起動し、従来の「Start Scanning」ボタンを押す手間を省く。
    // 再起動抑止: alreadyAutoLaunched で初回のみ自動起動、キャンセル時はボタンで再起動可能。
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

    // permission付与後に自動起動（初回のみ、scannedPagesが空のとき=新規スキャン）
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !alreadyAutoLaunched && scannedPages.isEmpty() && !isProcessing) {
            alreadyAutoLaunched = true
            launchScanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Edge-to-Edge: StatusBar scrim/List章 検証用 — TopAppBarは自動でsafeDrawingを処理
            TopAppBar(
                title = { Text("Scan Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedPages.isNotEmpty() && !isProcessing) {
                        TextButton(onClick = { 
                            android.util.Log.d("ScanScreen", "Top Bar Done clicked")
                            viewModel.processScans(deckId) { newId -> onComplete(newId) }
                        }) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (scannedPages.isNotEmpty() && !isProcessing) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { 
                            android.util.Log.d("ScanScreen", "Bottom Finish button clicked")
                            viewModel.processScans(deckId) { newId -> onComplete(newId) } 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
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
                        Button(onClick = { launchScanner() }) {
                            Text("Start Scanning")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Scanner will open automatically", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    contentPadding = PaddingValues(8.dp),
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

// TODO(IMP-05 5-7): @FormFactorPreviews 適用手順
// ```
// @FormFactorPreviews
// @Composable
// fun ScanScreenPreview() { MaterialTheme { ScanScreen(onBack={}, onComplete={}, deckId=0) } }
// ```
// 既に LazyVerticalGrid(GridCells.Adaptive(120.dp)) 適用済 — IMP-05 Step4の参考実装。
// FormFactorPreviews で 120dp列の可変を Tablet/Desktop で検証すること。

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

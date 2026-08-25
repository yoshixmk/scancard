package com.plath.scancard.ui.extraction

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import androidx.work.WorkInfo
// TODO(IMP-05 5-7): @FormFactorPreviews 適用用 import（コメント留め — 有効化時にアンコメント）
// import com.plath.scancard.ui.preview.FormFactorPreviews

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
    val extractionWorkInfo by viewModel.extractionWorkInfo.collectAsState()

    val ctx = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // POST_NOTIFICATIONS 拒否でも抽出は継続（通知は SecurityException catchで握り潰し）
        // 権限は通知表示の任意要件のため、isGranted に関わらず startExtraction する
        if (!isGranted) {
            android.util.Log.w("ExtractionPreview", "POST_NOTIFICATIONS denied — extraction continues without notification")
        }
        android.util.Log.d("ExtractionPreview", "Permission result isGranted=$isGranted, starting extraction for deck $deckId")
        viewModel.startExtraction(deckId)
    }

    LaunchedEffect(deckId) {
        android.util.Log.d("ExtractionPreview", "Screen loaded with deckId: $deckId")
        viewModel.setDeckId(deckId)
    }
    
    // Auto-finish when extraction completes
    LaunchedEffect(extractionWorkInfo) {
        if (extractionWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
            onFinish(deckId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Extraction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        // edge-to-edge: navigation barめり込み防止 — safeDrawingをcontentWindowInsetsに指定し、innerPaddingをcontentPaddingへ委譲
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(16.dp)
                // navigationBar分の追加padding: ScaffoldのcontentWindowInsetsがbottomを処理するが、scroll末尾のボタンが隠れないようwindowInsetsBottomを保証
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("Select AI Model", style = MaterialTheme.typography.titleLarge)
            Text("Delivered via Google Play AI Delivery", style = MaterialTheme.typography.bodySmall)
            
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
                    Button(onClick = { viewModel.downloadModel() }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download via Google Play")
                    }
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        val ctx = LocalContext.current
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    java.io.File(ctx.filesDir, selectedModel.fileName).writeText("dummy")
                                    android.util.Log.d("ExtractionPreview", "Created dummy model file for E2E")
                                } catch (e: Exception) {
                                    android.util.Log.e("ExtractionPreview", "Failed to create dummy", e)
                                }
                                viewModel.checkModelStatus()
                            },
                            modifier = Modifier.testTag("createDummyModelBtn")
                        ) {
                            Text("Create Dummy Model (E2E)")
                        }
                    }
                }
                is ModelState.Downloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(progress = { state.progress })
                        Text("Downloading ${selectedModel.name}... ${(state.progress * 100).toInt()}%")
                        Text("Please wait, this may take a few minutes.", style = MaterialTheme.typography.labelSmall)
                    }
                }
                is ModelState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    
                    if (state.message.contains("-1")) {
                        Text(
                            "Dev Tip: Play AI Delivery requires Play Store installation. " +
                            "For development, push the model file to: /data/data/com.plath.scancard/files/${selectedModel.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(onClick = { viewModel.checkModelStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Status Check")
                    }
                }
                is ModelState.Ready -> {
                    // WorkInfo可視化: ENQUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED を明示し、ユーザーがステータスを迷わないようにする
                    val wiState = extractionWorkInfo?.state
                    if (isExtracting || wiState == WorkInfo.State.ENQUEUED) {
                        CircularProgressIndicator()
                        Text("Gemma is extracting flashcards in background... (${wiState?.name ?: "RUNNING"})")
                        Text("You can safely leave this screen.", style = MaterialTheme.typography.labelSmall)
                        if (extractionWorkInfo?.progress?.getInt("progress", -1) != -1) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    } else if (wiState == WorkInfo.State.SUCCEEDED) {
                        Text("Extraction complete — navigating to deck…", color = MaterialTheme.colorScheme.primary)
                        CircularProgressIndicator()
                    } else if (wiState == WorkInfo.State.FAILED) {
                        Text("Extraction failed.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            android.util.Log.d("ExtractionPreview", "Retry clicked for deck $deckId")
                            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else true
                            if (hasPerm) viewModel.startExtraction(deckId) else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Retry Extraction")
                        }
                    } else if (wiState == WorkInfo.State.CANCELLED) {
                        Text("Previous extraction was cancelled. Please retry.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            android.util.Log.d("ExtractionPreview", "Retry (cancelled) clicked for deck $deckId")
                            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else true
                            if (hasPerm) viewModel.startExtraction(deckId) else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Retry Extraction")
                        }
                    } else if (error != null) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            android.util.Log.d("ExtractionPreview", "Retry (error) clicked for deck $deckId")
                            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else true
                            if (hasPerm) viewModel.startExtraction(deckId) else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Retry Extraction")
                        }
                    } else {
                        Text("Model Ready: ${selectedModel.name}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (wiState == null) {
                            Text("No extraction running. Tap below to start.", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                android.util.Log.d("ExtractionPreview", "Start clicked for deck $deckId")
                                val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else true
                                if (hasPerm) viewModel.startExtraction(deckId) else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extractionStartBtn")
                                // edge-to-edge: ボタンがnavigation barにめり込まないよう追加Bottom paddingはScaffoldのwindowInsetsで保証済み、ここではnavigationBarsPaddingを重複しない
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
}

// TODO(IMP-05 5-7): @FormFactorPreviews 適用手順
// ```
// @FormFactorPreviews
// @Composable
// fun ExtractionPreviewScreenPreview() { MaterialTheme { ExtractionPreviewScreen(onBack={}, onFinish={}, deckId=1) } }
// ```

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
                    Text("Estimated Size: ${selectedModel.sizeGb} GB", style = MaterialTheme.typography.labelSmall)
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
                            Text("Estimated Size: ${model.sizeGb} GB", style = MaterialTheme.typography.labelSmall)
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

package com.plath.scancard.ui.export

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// TODO(IMP-05 5-7): Imports for applying @FormFactorPreviews (commented out — uncomment when enabling)
// import com.plath.scancard.ui.preview.FormFactorPreviews

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
        // SKILL.md Step3: contentWindowInsets=safeDrawing handles systemBars; bottom uses windowInsetsPadding to avoid double padding
        contentWindowInsets = WindowInsets.safeDrawing,
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
        Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = selectedFormat == "TSV",
                    onClick = {
                        selectedFormat = "TSV"
                        viewModel.generatePreview("TSV")
                    },
                    label = { Text("TSV") },
                    modifier = Modifier.testTag("exportChipTSV")
                )
                FilterChip(
                    selected = selectedFormat == "CSV",
                    onClick = {
                        selectedFormat = "CSV"
                        viewModel.generatePreview("CSV")
                    },
                    label = { Text("CSV") },
                    modifier = Modifier.testTag("exportChipCSV")
                )
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.weight(1f).fillMaxWidth().testTag("exportPreviewCard")) {
                Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = exportText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("exportPreviewText")
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // TODO(IMP-05 5-7): @FormFactorPreviews application procedure
            // ```
            // @FormFactorPreviews
            // @Composable
            // fun ExportScreenPreview() { MaterialTheme { ExportScreen(onBack={}) } }
            // ```

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ScanCard Export", exportText)
                        clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier.testTag("exportCopyBtn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        val intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, exportText)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Export"))
                    },
                    modifier = Modifier.testTag("exportShareBtn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

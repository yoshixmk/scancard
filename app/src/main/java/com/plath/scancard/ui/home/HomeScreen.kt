package com.plath.scancard.ui.home

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plath.scancard.data.local.entities.Deck
// TODO(IMP-05): Adaptive Grid 移行用 import（コメント留め — 有効化時にアンコメント）
// import androidx.compose.foundation.lazy.grid.GridCells
// import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// import androidx.compose.foundation.lazy.grid.items
// import com.plath.scancard.ui.preview.FormFactorPreviews

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
        // Edge-to-Edge SKILL.md Step2-3: Scaffold contentWindowInsets=safeDrawingで systemBars を処理、
        // Adaptive: NavigationSuiteScaffold は PaddingValuesを伝播しないため個別画面で insets 処理が必要
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text("ScanCard") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                modifier = Modifier.testTag("homeFabScan")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp).fillMaxWidth().testTag("homeCreateDeckBtn")
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
                // TODO(IMP-05): LazyColumn → LazyVerticalGrid への adaptive 置換案
                // 現状は IMP-03 の contentPadding 維持のため LazyColumn のまま。Foldable/Tablet で
                // 列数自動可変させる場合は下記に置換（ビルドを壊さないためコメント留め、段階的適用）:
                // ```
                // LazyVerticalGrid(
                //     columns = GridCells.Adaptive(320.dp),
                //     modifier = Modifier.weight(1f),
                //     contentPadding = padding, // IMP-03 の edge-to-edge 対応を維持
                //     verticalArrangement = Arrangement.spacedBy(0.dp),
                //     horizontalArrangement = Arrangement.spacedBy(0.dp)
                // ) {
                //     items(decks) { deck ->
                //         DeckItem(deck = deck, onClick = { onDeckClick(deck.id) }, onDelete = { viewModel.deleteDeck(deck) })
                //     }
                // }
                // ```
                // 有効化手順: 上部 import のコメントを外し、この LazyColumn ブロックを上記で置換。
                // 既存 Preview (HomeScreenPreview) は @FormFactorPreviews で 4形態検証すること。
                // SKILL.md Lists章: contentPaddingに innerPadding を渡し先頭/末尾をsystemBarsから離す。
                // 親Columnが Modifier.padding(padding) で既に inset しているため top は二重にならないよう bottom のみを contentPadding に委譲し FAB分の余白を加算
                LazyColumn(
                    modifier = Modifier.weight(1f).consumeWindowInsets(padding),
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 80.dp)
                ) {
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
                val isError = newDeckTitle.length > 100
                TextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    label = { Text("Title") },
                    isError = isError,
                    supportingText = {
                        if (isError) Text("Title must be 1-100 characters")
                    },
                    modifier = Modifier.imePadding().testTag("newDeckTitleInput")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createDeck(newDeckTitle)
                        newDeckTitle = ""
                        showAddDialog = false
                    },
                    enabled = newDeckTitle.isNotBlank() && newDeckTitle.length <= 100,
                    modifier = Modifier.testTag("newDeckCreateBtn")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// TODO(IMP-05 5-7): 各Screen Preview に @FormFactorPreviews 適用
// 手順: 下記プレビュー雛形を有効化し、電話/折畳/タブレット/デスクトップの4形態を同時検証
// ```
// @FormFactorPreviews
// @Composable
// fun HomeScreenPreview() {
//     MaterialTheme { HomeScreen(onScanClick = {}, onDeckClick = {}) }
// }
// @FormFactorPreviews
// @Composable
// fun DeckItemPreview() {
//     MaterialTheme { DeckItem(deck = Deck(id=1, title="Sample", createdAt=Date()), onClick={}, onDelete={}) }
// }
// ```
// 注意: adaptive skill Step1準拠 — FormFactorPreviews は ui.preview.FormFactorPreviews.kt で定義

// IMP-09 Styles API experimental - see .kiro/skills/styles

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
            .testTag("deckCard_${deck.title}")
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
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("deckDeleteBtn_${deck.title}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

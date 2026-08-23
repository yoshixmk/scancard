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
            FloatingActionButton(onClick = onScanClick) {
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
                modifier = Modifier.padding(16.dp).fillMaxWidth()
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
                TextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.imePadding()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createDeck(newDeckTitle)
                        newDeckTitle = ""
                        showAddDialog = false
                    }
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

// TODO(IMP-09) 9-3 Styles API 移行案 — DeckItem を style: Style パラメータ化する雛形 (コメントのみ、ビルド非破壊)
// 背景: .kiro/skills/styles/SKILL.md Step 3: Migrate a component to Styles API / Limitations 準拠。
//       Material3 Cardは Styles非対応だが、カスタムラップした DeckItem は対象可 (カスタムコンポーネントのみ)。
//       現行 DeckItem は Card + Row 直書き。将来 Styleへ移行する際は下記手順で視覚差分0を担保する。
// 移行手順 (有効化時: foundation:1.12.0-alpha01 + opt-in 後にアンコメント):
//   1. ComponentStyles.kt のコメント雛形を有効化し ComponentStyles.cardStyle を参照可能にする
//      (app/build.gradle.kts:TODO(IMP-09) Option A/B/C を先に適用)
//   2. 下記 DeckItem シグネチャを styleパラメータ化 (デフォルトは必ず `Style` 空スタイル):
//      ```
//      // import androidx.compose.foundation.style.Style
//      // import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
//      // import androidx.compose.foundation.style.styleable
//      // import androidx.compose.foundation.style.rememberUpdatedStyleState
//      // import androidx.compose.foundation.interaction.MutableInteractionSource
//      // import androidx.compose.runtime.remember
//      // @OptIn(ExperimentalFoundationStyleApi::class)
//      // @Composable
//      // fun DeckItem(
//      //     deck: Deck,
//      //     onClick: () -> Unit,
//      //     onDelete: () -> Unit,
//      //     modifier: Modifier = Modifier,
//      //     style: Style = Style, // 重要: デフォルトは Style (空)、ChipStyleDefault等の特定値を使わない (SKILL.md Step 3.3)
//      //     enabled: Boolean = true
//      // ) {
//      //     val interactionSource = remember { MutableInteractionSource() }
//      //     val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = enabled }
//      //     Card(
//      //         modifier = modifier
//      //             .fillMaxWidth()
//      //             .padding(horizontal = 16.dp, vertical = 8.dp)
//      //             .styleable(styleState, ComponentStyles.cardStyle, style) // 既存 background/shape/textStyle を Styleへ移行
//      //             .clickable(onClick = onClick, indication = null, interactionSource = interactionSource)
//      //     ) {
//      //         Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//      //             Column(modifier = Modifier.weight(1f)) {
//      //                 Text(text = deck.title, style = MaterialTheme.typography.titleMedium) // 将来は Styleの textStyleへ移行可
//      //                 Text(text = "Created: ...", style = MaterialTheme.typography.bodySmall)
//      //             }
//      //             IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
//      //         }
//      //     }
//      // }
//      ```
//      注意: remove個別スタイリング params (backgroundColor/shape/textStyle等) → style一本化。Modifier.styleable()でrootに適用。
//            clickableは indication=null + interactionSource共有で styleの pressed/disabled 状態を連動 (SKILL.md state例参照)。
//   3. 既存ハードコード (Modifier.padding/background等) を ComponentStyles.cardStyle へ移譲した後、DeckItem呼び出し側は変更不要
//      (デフォルト Style のため既存 `DeckItem(deck, onClick, onDelete)` は互換)。カスタム上書き時のみ `DeckItem(..., style = Style{ background(...) })` を渡す。
// Previewビジュアル差分なし確認手順 (gradleフル実行禁止のため手動/将来自動化):
//   A. 現状ベースライン取得 (Styles未適用の現在でスクショ):
//      - エミュレータ可用時: HomeScreenPreview / DeckItemPreview を Compose Preview + screenshotTest で撮影 (IMP-06 参照)
//        `./gradlew :app:validateScreenshotTest` or Paparazzi/Roborazzi で home_list の PNGを保存
//      - エミュレータ不可時: このステップはスキップし Step Bへ (SKILL.md Step 3.1 If you CANNOT run an emulator: Skip)
//   B. 移行後比較:
//      - 同一Previewで DeckItem(style=Style) と DeckItem() のレンダリングを並置し目視差分なしを確認 (文字列以外のレイアウト/色/shapeに注目)
//      - 9サイズ (400/610/900dp × 400/500/1000dp) + dark/light + fontScale1.5 で差分0 (docs/styles-migration.md 4. Validate参照)
//      - 差分があれば ComponentStyles.cardStyle の dp/color を既存ハードコード値に合わせて反復
//   C. UIテスト追加 (将来):
//      ```
//      // @Test fun deckItem_styleDefault_rendersSameAsBefore() {
//      //     composeRule.setContent { MaterialTheme { DeckItem(deck=sampleDeck(), onClick={}, onDelete={}) } }
//      //     composeRule.onNodeWithText("Sample").assertIsDisplayed()
//      //     // スクショ比較: assertAgainstGolden("deckItem_default")
//      // }
//      ```
//   D. ビルド検証: `./gradlew :app:assembleDebug` SUCCESS、既存Theme (Theme.kt) 無変更のため他画面影響なし
// ロールバック: 上記 styleパラメータ化を再コメントし元の DeckItem シグネチャに戻せば即時復帰。ComponentStylesはダミーobjectのまま残置可。
// 参考: app/src/main/java/com/plath/scancard/ui/theme/ComponentStyles.kt、docs/styles-migration.md、.kiro/skills/styles/SKILL.md Step 3-4

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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

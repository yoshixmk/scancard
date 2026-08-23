package com.plath.scancard.ui.theme

/**
 * IMP-09 Styles API 実験導入雛形 — コメント中心、コンパイルエラーを避けるためダミーオブジェクトのみ実体化。
 *
 * 現状: Material3直書き (Theme.kt は MaterialTheme のみ)、foundation 1.12.0-alpha01 未導入のため Style API は無効。
 *       本ファイルは将来の移行に備えたコメント雛形。gradleフル実行禁止のため依存追加は app/build.gradle.kts:TODO(IMP-09) にコメント留め。
 *
 * 前提 (有効化時):
 *   - app/build.gradle.kts で compileSdk=37 維持
 *   - BOM 2026.04.01+ または foundation:1.12.0-alpha01 を dependencies に追加
 *   - kotlin { compilerOptions { freeCompilerArgs.add("-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi") } }
 *   - import androidx.compose.foundation.style.Style / StyleScope / ExperimentalFoundationStyleApi が解決可能
 *
 * 適用範囲: カスタムコンポーネントのみ。Material3 (Card/Button/TextField 等) は対象外 (.kiro/skills/styles/SKILL.md Limitations)。
 *           既存Theme (Theme.kt: ScanCardTheme) は壊さない — ComponentStyles は static参照で提供し CompositionLocalは使わない。
 *
 * 有効化後は本ファイルのコメント雛形をアンコメントし、下記ダミー object を置換する。
 * 検証: ./gradlew :app:assembleDebug + Previewスクショ差分0 (docs/styles-migration.md 参照)。
 */

// ---------------------------------------------------------------------------
// 実体: ダミー (コンパイルエラー回避用) — 有効化時に下記コメント雛形で置換
// ---------------------------------------------------------------------------

/**
 * ダミー ComponentStyles — 現行ビルドでは Style API 未導入のため空objectでプレースホルダ。
 * 将来 Style API 有効化時に下のコメント雛形で完全置換する。
 */
object ComponentStyles {
    // placeholder: 将来 val cardStyle: Style 等を追加
    // 現状は参照なし。HomeScreen.kt:DeckItem等のハードコード色/shapeは維持。
}

// ---------------------------------------------------------------------------
// コメント雛形: Styles API 有効化時に本ブロックをアンコメントし上記ダミーを置換
// ---------------------------------------------------------------------------

// // 有効化時に必要な import (依存追加後にアンコメント):
// // import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
// // import androidx.compose.foundation.style.Style
// // import androidx.compose.foundation.style.StyleScope
// // import androidx.compose.foundation.style.background
// // import androidx.compose.foundation.style.shape
// // import androidx.compose.foundation.style.minWidth
// // import androidx.compose.foundation.style.minHeight
// // import androidx.compose.foundation.style.textStyle
// // import androidx.compose.foundation.style.disabled
// // import androidx.compose.material3.MaterialTheme
// // import androidx.compose.ui.unit.dp
// // import androidx.compose.runtime.Composable
// // import androidx.compose.runtime.ReadOnlyComposable
//
// /**
//  * 実験的API注意: Styles API は @ExperimentalFoundationStyleApi。破壊的変更の可能性あり。
//  * 本objectは Theme.kt の ScanCardTheme から static参照でアクセスする想定 (CompositionLocal不使用)。
//  * 参照: .kiro/skills/styles/SKILL.md Step 2: Establish ComponentStyles
//  */
// // @OptIn(ExperimentalFoundationStyleApi::class)
// // object ComponentStyles {
// //     /**
// //      * DeckItem / カード系カスタムスタイル雛形。
// //      * 既存 DeckItem (HomeScreen.kt:152-174) のハードコード padding/background/shape を Styleへ移行する例。
// //      * ハードコード除去対象: backgroundColor, shape, textStyle, contentPadding, minWidth/minHeight等
// //      */
// //     val cardStyle: Style = Style {
// //         // Themeトークンを StyleScope経由で参照 (Local例: LocalJetsnackTheme.current.colors の代替)
// //         // ScanCardは MaterialTheme の colorScheme/typography/shapes を使用:
// //         //   colors = MaterialTheme.colorScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes
// //         // StyleScope拡張で themeトークンを公開する場合は下記拡張を Theme.kt近傍に定義:
// //         //   val StyleScope.colors get() = LocalScanCardTheme.current.colors  (ScanCardでは MaterialThemeを直接参照可)
// //         background(MaterialTheme.colorScheme.surfaceVariant)
// //         shape(MaterialTheme.shapes.medium)
// //         minWidth(280.dp)
// //         minHeight(72.dp)
// //         textStyle(MaterialTheme.typography.titleMedium)
// //         // 状態別スタイル (pressed/disabled等) は Modifier.styleable + MutableStyleState で制御:
// //         disabled {
// //             background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
// //         }
// //     }
// //
// //     /**
// //      * ボタン系カスタムスタイル雛形 — CustomButton移行例 (SKILL.md Step 3 Migration example準拠)。
// //      * 有効化前: backgroundColor/shape/textStyle をパラメータで直渡し
// //      * 有効化後: style: Style = Style に一本化し Style内で一元管理
// //      */
// //     val buttonStyle: Style = Style {
// //         background(MaterialTheme.colorScheme.primary)
// //         shape(MaterialTheme.shapes.extraLarge)
// //         minWidth(58.dp)
// //         minHeight(40.dp)
// //         textStyle(MaterialTheme.typography.labelLarge)
// //         disabled {
// //             background(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
// //         }
// //     }
// //
// //     /**
// //      * テキストフィールド等の追加スタイルが必要な場合に拡張。
// //      * Material3 TextField自体は Styles非対応のため、ラップしたカスタムコンポーネントにのみ適用。
// //      */
// //     val customTextFieldStyle: Style = Style {
// //         background(MaterialTheme.colorScheme.surface)
// //         shape(MaterialTheme.shapes.small)
// //         textStyle(MaterialTheme.typography.bodyMedium)
// //     }
// // }
//
// /**
//  * StyleScope から themeトークンを参照するための拡張雛形 (任意)。
//  * ScanCardでは MaterialTheme を直接参照できるが、カスタムテーマ (Jetsnack例) では CompositionLocal経由で公開:
//  *
//  *   val StyleScope.colors: ColorScheme get() = MaterialTheme.colorScheme
//  *   val StyleScope.typography: Typography get() = MaterialTheme.typography
//  *   val StyleScope.shapes: Shapes get() = MaterialTheme.shapes
//  *
//  * 有効化時は Theme.kt 近傍に配置し、ComponentStyles内の Style定義で colors/typography/shapes を直接使えるようにする。
//  */
//
// /**
//  * Theme 側からの static参照公開雛形 (SKILL.md Step 2.3)。
//  * ScanCardTheme に companion 的な styles 参照を追加する例:
//  *
//  *   object ScanCardTheme {
//  *       val styles: ComponentStyles get() = ComponentStyles
//  *       // 既存の colors/typography/shapes も同様に公開可
//  *   }
//  *
//  * ただし現行 Theme.kt は @Composable fun ScanCardTheme 形式のため、object化せず
//  * Composable外から ComponentStyles を直接参照する方式でも可 (CompositionLocal不要)。
//  */

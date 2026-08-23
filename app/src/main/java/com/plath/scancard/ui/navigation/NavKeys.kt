package com.plath.scancard.ui.navigation

import kotlinx.serialization.Serializable

/**
 * IMP-02 Navigation3 NavKey 雛形 (ビルドを壊さないためコメント中心)
 *
 * 背景: `ui/ScanCardNavHost.kt` は navigation-compose 2.9.8 の string route (Screen.kt) を使用。
 *       Navigation3 移行では各 destination を `@Serializable` + `NavKey` 化し、
 *       `rememberNavBackStack(Home)` / `NavDisplay(entryProvider {...})` で管理する。
 *       JVM8環境では navigation3 依存追加でビルド不可のため、本ファイルは
 *       `navigation3-runtime` を import せず `kotlinx.serialization.Serializable` のみで雛形化している。
 *
 * 有効化手順 (JVM17 + AGP 9.3.1 で実行):
 *   1. app/build.gradle.kts の TODO(IMP-02) コメントを外し Navigation3 依存を Sync
 *      - implementation("androidx.navigation3:navigation3-runtime:1.0.0")
 *      - implementation("androidx.navigation3:navigation3-ui:1.0.0")
 *      - plugins { id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" }
 *   2. 下記各クラスのコメントを外す:
 *      - `import androidx.navigation3.runtime.NavKey` を追加
 *      - 各 `data object / data class` に `: NavKey` を付与
 *        例: `@Serializable data object Home : NavKey` / `@Serializable data class DeckDetail(val deckId: Long) : NavKey`
 *   3. ui/Screen.kt (string route) は NavKeys.kt に置換されるため削除 or Deprecated化
 *   4. ui/ScanCardNavHost.kt を docs/navigation3-migration.md の After 例に置換
 *
 * 参考:
 *   - .kiro/skills/navigation-3/SKILL.md migration-guide Step2
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step2
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicdsl.md
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicsaveable.md
 *
 * 注意: 現状は Dummy data class としてコンパイルが通ることを保証。navigation3 の NavKey interface はコメントで代替。
 */

// TODO(IMP-02): Navigation3 有効化時は `import androidx.navigation3.runtime.NavKey` を追加し、各クラスに `: NavKey` を付与
// import androidx.navigation3.runtime.NavKey

@Serializable
data object Home // : NavKey

@Serializable
data class Scan(val deckId: Long) // : NavKey

@Serializable
data class ExtractionPreview(val deckId: Long) // : NavKey

@Serializable
data class DeckDetail(val deckId: Long) // : NavKey

@Serializable
data class Study(val deckId: Long) // : NavKey

@Serializable
data class Export(val deckId: Long) // : NavKey

// 将来的な拡張例 (必要に応じて追加):
// @Serializable data object Settings : NavKey
// @Serializable data class Search(val query: String? = null) : NavKey
//   - nullable 引数は Navigation3 でも自動サポート (type-safe-destinations.md Best practices参照)
//   - 複雑な型は custom NavType を定義 (type-safe-destinations.md Step5 参照)

// 旧 Screen.kt との対応表 (移行時に置換):
// Screen.Home.route ("home")                          -> Home
// Screen.Scan.route ("scan/{deckId}")                 -> Scan(deckId)
// Screen.ExtractionPreview.route                      -> ExtractionPreview(deckId)
// Screen.DeckDetail.route + deepLink scancard://deck/{deckId} -> DeckDetail(deckId)
// Screen.Study.route                                  -> Study(deckId)
// Screen.Export.route                                 -> Export(deckId)

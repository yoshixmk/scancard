package com.plath.scancard.ui.navigation

import kotlinx.serialization.Serializable

/**
 * IMP-02 Navigation3 NavKey Template (Comment-based to avoid breaking the build)
 *
 * Background: `ui/ScanCardNavHost.kt` uses string routes from navigation-compose 2.9.8 (Screen.kt).
 *       In Navigation3 migration, each destination becomes `@Serializable` + `NavKey`,
 *       managed by `rememberNavBackStack(Home)` / `NavDisplay(entryProvider {...})`.
 *       Since build is impossible with Navigation3 dependencies in JVM8 environments,
 *       this file is templated with only `kotlinx.serialization.Serializable` without
 *       importing `navigation3-runtime`.
 *
 * Activation Procedure (Run with JVM17 + AGP 9.3.1):
 *   1. Uncomment TODO(IMP-02) in `app/build.gradle.kts` and Sync Navigation3 dependencies
 *      - implementation("androidx.navigation3:navigation3-runtime:1.0.0")
 *      - implementation("androidx.navigation3:navigation3-ui:1.0.0")
 *      - plugins { id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" }
 *   2. Uncomment each class below:
 *      - add `import androidx.navigation3.runtime.NavKey`
 *      - append `: NavKey` to each `data object / data class`
 *        Example: `@Serializable data object Home : NavKey` / `@Serializable data class DeckDetail(val deckId: Long) : NavKey`
 *   3. `ui/Screen.kt` (string route) will be replaced by NavKeys.kt, so delete or mark as Deprecated.
 *   4. Replace `ui/ScanCardNavHost.kt` with the After example in `docs/navigation3-migration.md`.
 *
 * References:
 *   - .kiro/skills/navigation-3/SKILL.md migration-guide Step2
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step2
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicdsl.md
 *   - .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/recipes/basicsaveable.md
 *
 * Note: Currently ensures compilation as dummy data classes. Navigation3's NavKey interface is substituted with comments.
 */

// TODO(IMP-02): When enabling Navigation3, add `import androidx.navigation3.runtime.NavKey` and append `: NavKey` to each class
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

// Future expansion examples (add as needed):
// @Serializable data object Settings : NavKey
// @Serializable data class Search(val query: String? = null) : NavKey
//   - Nullable arguments are automatically supported in Navigation3 (see type-safe-destinations.md Best practices)
//   - Define custom NavType for complex types (see type-safe-destinations.md Step 5)

// Correspondence table with old Screen.kt (replace upon migration):
// Screen.Home.route ("home")                          -> Home
// Screen.Scan.route ("scan/{deckId}")                 -> Scan(deckId)
// Screen.ExtractionPreview.route                      -> ExtractionPreview(deckId)
// Screen.DeckDetail.route + deepLink scancard://deck/{deckId} -> DeckDetail(deckId)
// Screen.Study.route                                  -> Study(deckId)
// Screen.Export.route                                 -> Export(deckId)

package com.plath.scancard.ui.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * IMP-05 Step 1: FormFactor Previews for Adaptive UI verification
 *
 * Adheres to adaptive/SKILL.md Step 1 — Previews 4 form factors: Phone / Foldable / Tablet / Desktop
 * with a single annotation.
 *
 * Example usage:
 * ```
 * @FormFactorPreviews
 * @Composable
 * fun HomeScreenPreview() {
 *     MaterialTheme { HomeScreen(...) }
 * }
 * ```
 * Existing @Preview can coexist without replacement. Also useful in Screenshot Testing (IMP-06)
 * when combined with @PreviewTest.
 *
 * Device sizes specified in improvement.md IMP-05 / Task 5-2:
 * - PHONE    400dp x 800dp
 * - FOLDABLE 700dp x 800dp (representative value for unfolded foldable)
 * - TABLET   900dp x 1200dp
 * - DESKTOP  1200dp x 800dp
 */
@Preview(name = "Phone - 400x800", widthDp = 400, heightDp = 800, showBackground = true)
@Preview(name = "Foldable - 700x800", widthDp = 700, heightDp = 800, showBackground = true)
@Preview(name = "Tablet - 900x1200", widthDp = 900, heightDp = 1200, showBackground = true)
@Preview(name = "Desktop - 1200x800", widthDp = 1200, heightDp = 800, showBackground = true)
annotation class FormFactorPreviews

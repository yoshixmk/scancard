package com.example.scancard.ui.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * IMP-05 Step1: Adaptive UI 検証用 FormFactor Previews
 *
 * adaptive/SKILL.md Step1 準拠 — Phone / Foldable / Tablet / Desktop の4形態を
 * 単一アノテーションでプレビューする。
 *
 * 使用例:
 * ```
 * @FormFactorPreviews
 * @Composable
 * fun HomeScreenPreview() {
 *     MaterialTheme { HomeScreen(...) }
 * }
 * ```
 * 既存の @Preview は置換せず併記可能。Screenshot Testing (IMP-06) でも
 * このアノテーションを @PreviewTest と組み合わせて使用する想定。
 *
 * Device サイズは improvement.md IMP-05 / Task 5-2 指定:
 * - PHONE    400dp x 800dp
 * - FOLDABLE 700dp x 800dp (unfolded foldable の代表値)
 * - TABLET   900dp x 1200dp
 * - DESKTOP  1200dp x 800dp
 */
@Preview(name = "Phone - 400x800", widthDp = 400, heightDp = 800, showBackground = true)
@Preview(name = "Foldable - 700x800", widthDp = 700, heightDp = 800, showBackground = true)
@Preview(name = "Tablet - 900x1200", widthDp = 900, heightDp = 1200, showBackground = true)
@Preview(name = "Desktop - 1200x800", widthDp = 1200, heightDp = 800, showBackground = true)
annotation class FormFactorPreviews

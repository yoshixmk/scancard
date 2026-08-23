package com.example.scancard.domain.util

import com.example.scancard.data.local.entities.Card

class ExportManager {
    // IMP-07 7-5: design.md 641-662 TSVエスケープ確認
    // 現行は lossy 置換（tab/newline -> space）でデリミタ衝突を回避。round-tripで元文字は復元されないが既存テスト互換。
    // 設計準拠の lossless エスケープは escapeTsv()（tab->\t, newline->\n, \r->\r）で可能。将来切替時は下記を使用し import側で unescape すること。
    // 確認コメント: 現行 exportToTSV は tab/newline を " " に置換し、split("\n")/split("\t") のパース破綻を防止していることは確認済み。
    // Clipboard+file save エラーハンドリング: 呼び出し側（UI層 ClipboardManager / SAF）で try/catch すること。
    //   - ClipboardManager#setPrimaryClip で SecurityException/ IllegalStateException を catch し Snackbar 表示
    //   - ファイル保存（SAF/ MediaStore）で IOException を catch、フォールバックとして Clipboard コピーを試行
    //   - 空リストは "" を返し UIで「エクスポートするカードがありません」表示
    private fun escapeTsv(s: String): String = s.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    @Suppress("unused")
    private fun unescapeTsv(s: String): String = s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r")

    fun exportToTSV(cards: List<Card>): String {
        return cards.joinToString("\n") { card ->
            // IMP-07 7-5: design準拠なら escapeTsv(card.term) を使用。現行は後方互換で space 置換 + \r 対応追加。
            val escapedTerm = card.term.replace("\t", " ").replace("\n", " ").replace("\r", " ")
            val escapedDef = card.definition.replace("\t", " ").replace("\n", " ").replace("\r", " ")
            // 将来の lossless 版: val escapedTerm = escapeTsv(card.term); val escapedDef = escapeTsv(card.definition)
            "$escapedTerm\t$escapedDef"
        }
    }
}

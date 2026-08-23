package com.plath.scancard.data.ml

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * IMP-10 10-1 監査結果 (2026-08-23) — camerax/SKILL.md threading / modern-apis / mlkit-spatial 照合
 *
 * [スレッド] 要改善 (非破壊コメントのみ):
 *   - InputImage.fromFilePath(context, uri) は ContentResolver.openInputStream を内部で呼び出しブロッキング I/O。
 *     現行は呼び出し元 Dispatcher に依存 (ScanDocumentUseCase.processScannedPages は viewModelScope=Main で実行)。
 *     Main スレッドでの I/O は ANR リスク。推奨は withContext(Dispatchers.IO) でラップ。
 *     ただし本監査では既存パイプラインを壊さないため修正せず、将来対応としてコメント留め。
 *     参考: camerax/SKILL.md "Thread safety: Camera callbacks often run on background executors. Dispatch UI updates on main thread."
 *   - recognizer.process(image).await() は Play Services Task -> coroutines-play-services の await() で
 *     内部は Main スレッドにコールバックを返すが suspend のため安全。
 *   - 修正案 (適用時は本コメントをコードに反映):
 *     ```
 *     suspend fun recognizeText(imageUri: Uri): String = withContext(Dispatchers.IO) {
 *         val image = InputImage.fromFilePath(context, imageUri) // IO thread
 *         recognizer.process(image).await().text
 *     }
 *     ```
 *
 * [Escaping / リソースリーク] 要注意:
 *   - recognizer は TextRecognition.getClient() で生成されたシングルトン的クライアント。
 *     close() が未実装のため Activity 再生成時にネイティブリソースが残留する可能性。
 *     推奨: @ActivityRetainedScoped や Application Scope で管理し、onCleared / Application.onTerminate で
 *     recognizer.close() を呼ぶ。現状は Hilt Singleton 相当ではないため将来的にスコープ明示を検討。
 *   - InputImage は Uri ベースで内部 Bitmap を保持しないため close 不要 (OK)。
 *
 * [Immutability / Builder再代入] OK:
 *   - JapaneseTextRecognizerOptions.Builder().build() は単一呼び出しで正しく build。
 *     Builder の setter が fluent で新インスタンスを返すパターンでも、本コードは単一 build のみで再代入漏れなし。
 *     camerax/references/immutability.md の Viewport/Recorder パターンには非該当だが原則は遵守。
 *
 * [ML Kit Spatial / 回転] 注意:
 *   - InputImage.fromFilePath は EXIF 回転を自動補正するが、CameraX ImageProxy を使う場合は
 *     MlKitAnalyzer が座標マッピング/回転/ミラーを自動処理する (modern-apis.md 推奨)。
 *     将来 CameraX ImageAnalysis に移行する際は MlKitAnalyzer に置換し、手動 ByteBuffer 演算を撤廃すること。
 *     現行の Uri 経由(JPEG)では回転は OS が処理するため OK。
 *
 * [Thermal] N/A 直接関連なし。OCR 自体は CPU 負荷だが docs/camera-thermals.md の
 *   "Mild tier: Stop background analysis" に該当 — 発熱時に OCR 解像度ダウングレード (1080p->720p) を検討。
 */
class TextRecognitionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(imageUri: Uri): String {
        // AUDIT NOTE: ブロッキング I/O なので本来は Dispatchers.IO でラップすべき (上記 KDoc 参照)。
        // 現行は既存呼び出し元への影響を避けるためそのまま。修正時は withContext(Dispatchers.IO) を追加。
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()
        return result.text
    }

    // AUDIT NOTE: 将来のリソースリーク対策 — ApplicationScope で close() を呼ぶ場合は下記を有効化:
    // fun close() { recognizer.close() }
}

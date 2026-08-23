package com.example.scancard.service

// =============================================================================
// IMP-08 AppFunctions Service 雛形 — コメント中心でコンパイルエラーを回避
// =============================================================================
// 要件: targetSdk 36 + compileSdk 37 + androidx.appfunctions 1.0.0-alpha10+ + KSP + Hilt
// skill: .kiro/skills/appfunctions/references/implementation-configuration.md Step4 / context.md
// 制約: gradle フル実行禁止のため依存は app/build.gradle.kts でコメント留め。
//       AppFunctions 依存が無い状態でも ./gradlew :app:assembleDebug が SUCCESS するよう
//       実装本体はブロックコメントで無効化し、ダミープレースホルダのみをコンパイル対象とする。
// =============================================================================

// ---------------------------------------------------------------------------
// 有効化手順 (targetSdk 36 昇格時にコメントを外す)
// ---------------------------------------------------------------------------
// 1. app/build.gradle.kts:14 の targetSdk を 36 に変更 (TODO(IMP-08) コメント参照)
// 2. app/build.gradle.kts dependencies の AppFunctions 2行のコメントを外し Sync:
//      implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10")
//      ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
//      ksp { arg("appfunctions.aggregateAppFunctions", "true") } // 必要に応じて
// 3. 本ファイルのブロックコメントを外し、下記 import/アノテーションを有効化
// 4. res/xml/app_metadata.xml を作成し AndroidManifest.xml に service/app_metadata を登録
// 5. JVM17 で ./gradlew :app:assembleDebug → 生成物 app/build/generated/ksp/debug/assets/*.xml を確認
// 6. adb shell cmd app_function list-app-functions で登録確認 (docs/appfunctions-discovery.md 4章)
// ---------------------------------------------------------------------------

// --- 有効化時にアンコメントする imports ---
// import android.net.Uri
// import android.os.Build
// import androidx.annotation.RequiresApi
// import androidx.appfunctions.AppFunctionData
// import androidx.appfunctions.AppFunctionException
// import androidx.appfunctions.AppFunctionInvalidArgumentException
// import androidx.appfunctions.AppFunctionElementNotFoundException
// import androidx.appfunctions.AppFunctionExecutionException
// import androidx.appfunctions.annotation.AppFunction
// import androidx.appfunctions.annotation.AppFunctionSerializable
// import androidx.appfunctions.service.AppFunctionService
// import androidx.appfunctions.service.AppFunctionServiceEntryPoint
// import dagger.hilt.android.AndroidEntryPoint
// import javax.inject.Inject
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
// import java.time.Instant

/**
 * Placeholder object to keep this file compilable without AppFunctions dependency.
 * 実装本体は下記ブロックコメントに雛形として保持。有効化時にコメントを外す。
 */
object ScanCardAppFunctionServicePlaceholder {
    const val SERVICE_NAME = "ScanCardAppFunctionService"
    const val XML_FILE_NAME = "scan_card_app_function_service"
    const val PACKAGE_NAME = "com.example.scancard"
    const val FUNCTION_IDS = "createDeck, scanAndExtract, searchCards, exportDeck"
}

/*
 * ===========================================================================
 * 有効化時にアンコメントする実装雛形 (Hilt + ServiceEntryPoint パターン)
 * ===========================================================================
 * // res/xml/app_metadata.xml 例:
 * // <AppFunctionAppMetadata xmlns:appfn="http://schemas.android.com/apk/androidx.appfunctions"
 * //     appfn:description="ScanCard manages vocabulary decks via OCR and on-device AI extraction.
 * //     Operational Patterns:
 * //     - Always call 'createDeck' to obtain deckId before 'scanAndExtract' or 'searchCards'.
 * //     - Use 'searchCards' to resolve human-readable terms to card IDs.
 * //     Constraints:
 * //     - Deck titles limited to 100 characters.
 * //     - scanAndExtract requires valid content URI and downloaded Gemma model."
 * //     appfn:displayDescription="@string/appfunctions_display_description" />
 *
 * // AndroidManifest.xml 登録例 (<application> 内):
 * // <service
 * //     android:name="com.example.scancard.service.ScanCardAppFunctionService"
 * //     android:permission="android.permission.BIND_APP_FUNCTION_SERVICE"
 * //     android:exported="true"
 * //     tools:targetApi="36">
 * //     <property android:name="android.app.appfunctions.schema" android:value="app_functions_schema.xsd" />
 * //     <property android:name="android.app.appfunctions.v2" android:value="scan_card_app_function_service.xml" />
 * //     <intent-filter><action android:name="android.app.appfunctions.AppFunctionService" /></intent-filter>
 * // </service>
 * // <property android:name="android.app.appfunctions.app_metadata" android:resource="@xml/app_metadata" />
 *
 * @RequiresApi(36)
 * @AndroidEntryPoint
 * @AppFunctionServiceEntryPoint(
 *     serviceName = "ScanCardAppFunctionService",
 *     appFunctionXmlFileName = "scan_card_app_function_service",
 * )
 * abstract class ScanCardAppFunctionService : AppFunctionService() {
 *
 *     // Hilt 注入 — 既存 UseCase/Repository を再利用し冗長な抽象化を作らない
 *     @Inject internal lateinit var manageDeckUseCase: com.example.scancard.domain.usecase.ManageDeckUseCase
 *     @Inject internal lateinit var extractCardsUseCase: com.example.scancard.domain.usecase.ExtractCardsUseCase
 *     @Inject internal lateinit var studyCardsUseCase: com.example.scancard.domain.usecase.StudyCardsUseCase
 *     @Inject internal lateinit var exportDataUseCase: com.example.scancard.domain.usecase.ExportDataUseCase
 *     // 必要に応じて ScanRepository / ModelRepository も注入
 *
 *     // -----------------------------------------------------------------------
 *     // Serializable 定義 — inline KDoc 必須 (KSP は class-level @param を無視)
 *     // -----------------------------------------------------------------------
 *
 *     /** Result of creating a deck. */
 *     @AppFunctionSerializable(isDescribedByKDoc = true)
 *     data class CreateDeckResult(
 *         /** Generated deck identifier. Example: 42. */
 *         val deckId: Long,
 *         /** Deck title as provided. Example: "TOEIC 800". */
 *         val title: String,
 *         /** Creation timestamp as Instant. */
 *         val createdAt: Instant,
 *     )
 *
 *     /** Summary of a vocabulary card. */
 *     @AppFunctionSerializable(isDescribedByKDoc = true)
 *     data class CardSummary(
 *         /** Card identifier. */
 *         val id: Long,
 *         /** English term. Example: "apple". */
 *         val term: String,
 *         /** English definition. Example: "a fruit". */
 *         val definition: String,
 *         /** Japanese translation. May be empty. Example: "りんご". */
 *         val japaneseTranslation: String,
 *         /** Learning status: NEW, LEARNING, or REVIEW. */
 *         val status: String,
 *     )
 *
 *     /** Result of scan and extraction. */
 *     @AppFunctionSerializable(isDescribedByKDoc = true)
 *     data class ScanAndExtractResult(
 *         /** Target deck identifier. */
 *         val deckId: Long,
 *         /** Number of cards extracted. Example: 5. */
 *         val extractedCount: Int,
 *         /** Extracted cards. */
 *         val cards: List<CardSummary>,
 *     )
 *
 *     /** Result of deck export. */
 *     @AppFunctionSerializable(isDescribedByKDoc = true)
 *     data class ExportDeckResult(
 *         /** Target deck identifier. */
 *         val deckId: Long,
 *         /** Format used: TSV or CSV. */
 *         val format: String,
 *         /** Exported text content. Tab/newline escaped per ExportManager rules. */
 *         val content: String,
 *         /** Number of cards exported. */
 *         val cardCount: Int,
 *     )
 *
 *     // -----------------------------------------------------------------------
 *     // AppFunctions — KDoc 最適化済み (docs/appfunctions-discovery.md 3章)
 *     // 全関数は suspend + withContext(Dispatchers.IO) で UI スレッドをブロックしない
 *     // -----------------------------------------------------------------------
 *
 *     /**
 *      * Create a new deck with the given title.
 *      * @param title Deck title between 1 and 100 characters. Example: "TOEIC 800". Blank or longer than 100 throws.
 *      * @return Created deck identifier and metadata. Contains deckId (e.g., 42), title, and createdAt Instant.
 *      * @throws AppFunctionInvalidArgumentException If title is blank or exceeds 100 characters. Suggest the user provide a shorter title.
 *      * @throws AppFunctionExecutionException If database insertion fails.
 *      */
 *     @AppFunction(isDescribedByKDoc = true)
 *     suspend fun createDeck(title: String): CreateDeckResult = withContext(Dispatchers.IO) {
 *         if (title.isBlank() || title.length > 100) {
 *             throw AppFunctionInvalidArgumentException("Title must be between 1 and 100 characters")
 *         }
 *         val deckId = manageDeckUseCase.createDeck(title)
 *         val deck = manageDeckUseCase.getDeck(deckId)
 *             ?: throw AppFunctionExecutionException("Failed to retrieve created deck id=$deckId")
 *         CreateDeckResult(deckId = deckId, title = deck.title, createdAt = Instant.ofEpochMilli(deck.createdAt))
 *     }
 *
 *     /**
 *      * Scan an image and extract vocabulary cards into the specified deck.
 *      * Required workflow: Call "createDeck" first to obtain a valid deckId.
 *      * @param deckId Deck identifier from createDeck. Example: 42.
 *      * @param imageUri Content URI of the image to scan. Example: "content://media/picker/123". Must grant read permission.
 *      * @param modelConfig Optional model identifier. Defaults to "default". If null, uses on-device default model.
 *      * @return Extraction result with deckId, extractedCount (e.g., 5), and list of CardSummary (term, definition, japaneseTranslation).
 *      * @throws AppFunctionElementNotFoundException If deckId does not exist. Suggest calling createDeck first.
 *      * @throws AppFunctionInvalidArgumentException If imageUri is invalid or permission denied. Suggest the user re-select the image.
 *      * @throws AppFunctionExecutionException If model not downloaded or extraction fails.
 *      */
 *     @AppFunction(isDescribedByKDoc = true)
 *     suspend fun scanAndExtract(
 *         deckId: Long,
 *         imageUri: Uri,
 *         modelConfig: String? = null,
 *     ): ScanAndExtractResult = withContext(Dispatchers.IO) {
 *         val deck = manageDeckUseCase.getDeck(deckId)
 *             ?: throw AppFunctionElementNotFoundException("Deck not found for id=$deckId")
 *         // imageUri の権限チェック — SecurityException は InvalidArgument にマップ
 *         // 注意: 実際の OCR→Gemma 抽出は ExtractCardsUseCase.extractAndSaveCards(deckId, modelConfig) に委譲。
 *         //       本雛形では scanRepository.getScansByDeck + Gemma の同期呼び出しを想定するが、
 *         //       画像URIからの直接抽出パイプラインは将来拡張 (ScanRepository.insertScan 後に extract)。
 *         try {
 *             // 例: extractCardsUseCase.extractAndSaveCards(deckId, ModelConfig(modelConfig ?: "default"))
 *             // 成功後は studyCardsUseCase.getCards(deckId).first() で抽出結果を取得
 *         } catch (e: SecurityException) {
 *             throw AppFunctionInvalidArgumentException("Cannot read imageUri: $imageUri — permission denied")
 *         } catch (e: IllegalStateException) {
 *             throw AppFunctionExecutionException("Model not found: $modelConfig — please download it first")
 *         }
 *         // ダミー返却 — 実装時は上記で得た cards を CardSummary にマップ
 *         ScanAndExtractResult(deckId = deckId, extractedCount = 0, cards = emptyList())
 *     }
 *
 *     /**
 *      * Search cards in a deck by query.
 *      * Required workflow: Call this before referencing card IDs in other actions.
 *      * @param deckId Deck identifier. Example: 42.
 *      * @param query Search string for term/definition. Example: "apple". If empty, returns 3 most recent cards.
 *      * @param filterStatus Optional status filter: "NEW", "LEARNING", or "REVIEW". Null means no filter.
 *      * @return List of CardSummary objects matching the query (max 20). Empty list if no matches.
 *      * @throws AppFunctionElementNotFoundException If deckId does not exist.
 *      */
 *     @AppFunction(isDescribedByKDoc = true)
 *     suspend fun searchCards(
 *         deckId: Long,
 *         query: String,
 *         filterStatus: String? = null,
 *     ): List<CardSummary> = withContext(Dispatchers.IO) {
 *         manageDeckUseCase.getDeck(deckId)
 *             ?: throw AppFunctionElementNotFoundException("Deck not found for id=$deckId")
 *         // 実装: studyCardsUseCase.getCards(deckId).first()
 *         //       .filter { it.term.contains(query.trim(), ignoreCase=true) || it.definition.contains(...) }
 *         //       .filter { filterStatus == null || it.status.name == filterStatus }
 *         //       .take(20).map { CardSummary(...) }
 *         emptyList()
 *     }
 *
 *     /**
 *      * Export all cards in a deck as TSV or CSV text.
 *      * @param deckId Deck identifier. Example: 42.
 *      * @param format Export format, either "TSV" or "CSV". Defaults to "TSV".
 *      * @return Export result with deckId, format, content (TSV/CSV string), and cardCount. Content is empty string if deck has no cards.
 *      * @throws AppFunctionElementNotFoundException If deckId does not exist.
 *      * @throws AppFunctionExecutionException If export fails.
 *      */
 *     @AppFunction(isDescribedByKDoc = true)
 *     suspend fun exportDeck(
 *         deckId: Long,
 *         format: String = "TSV",
 *     ): ExportDeckResult = withContext(Dispatchers.IO) {
 *         manageDeckUseCase.getDeck(deckId)
 *             ?: throw AppFunctionElementNotFoundException("Deck not found for id=$deckId")
 *         val normalizedFormat = format.uppercase().let { if (it == "CSV") "CSV" else "TSV" }
 *         val content = if (normalizedFormat == "CSV") {
 *             exportDataUseCase.exportToCsv(deckId)
 *         } else {
 *             exportDataUseCase.exportToTsv(deckId) // or getExportText
 *         }
 *         val cardCount = if (content.isBlank()) 0 else content.lines().size
 *         ExportDeckResult(deckId = deckId, format = normalizedFormat, content = content, cardCount = cardCount)
 *     }
 * }
 *
 * // ---------------------------------------------------------------------------
 * // 破壊的 action 確認ダイアログ手順 (Security 制約)
 * // ---------------------------------------------------------------------------
 * // deleteDeck / deleteCard は不可逆のため本IMPでは AppFunction として公開しない。
 * // 公開する場合の安全なパターン:
 * // 1. AppFunction は直接削除せず PendingIntent を返す:
 * //      @AppFunction fun requestDeleteDeck(deckId: Long): PendingIntent
 * //      → PendingIntent は MainActivity の確認ダイアログ (AlertDialog) を起動する Intent をラップ
 * // 2. ユーザがダイアログで「削除」をタップして初めて ManageDeckUseCase.deleteDeck が実行される
 * // 3. Agent には「User confirmation required — pendingIntent を起動して確認ダイアログを表示してください」
 * //    と KDoc/throws で伝える
 * // 4. 代替: AppFunction 内で削除前に AppFunctionExecutionException で確認を要求し、
 * //    2回目の呼び出しで confirmed=true パラメータを必須にするパターンも可
 * // いずれも「確認なしの破壊的実行は禁止」 (skill Critical constraints: Security)
 * //
 * // 例 (有効化時に参考):
 * // @AppFunction(isDescribedByKDoc = true)
 * // suspend fun requestDeleteDeck(deckId: Long): PendingIntent = withContext(Dispatchers.IO) {
 * //     val deck = manageDeckUseCase.getDeck(deckId) ?: throw AppFunctionElementNotFoundException(...)
 * //     // 確認ダイアログ用 Intent を PendingIntent 化して返却
 * //     // Intent(context, MainActivity::class.java).apply { action="confirm_delete_deck"; putExtra("deckId", deckId) }
 * // }
 *
 */

package com.plath.scancard.service

// =============================================================================
// IMP-08 AppFunctions Service Template — Comment-based to avoid compilation errors
// =============================================================================
// Requirements: targetSdk 36 + compileSdk 37 + androidx.appfunctions 1.0.0-alpha10+ + KSP + Hilt
// skill: .kiro/skills/appfunctions/references/implementation-configuration.md Step4 / context.md
// Constraints: Dependencies are commented out in app/build.gradle.kts to avoid full gradle execution.
//       The implementation body is disabled with block comments, making only the dummy placeholder
//       a compilation target so that ./gradlew :app:assembleDebug succeeds.
// =============================================================================

// ---------------------------------------------------------------------------
// Activation Procedure (Uncomment when upgrading to targetSdk 36)
// ---------------------------------------------------------------------------
// 1. Change targetSdk to 36 in app/build.gradle.kts:14 (see TODO(IMP-08))
// 2. Uncomment AppFunctions lines in app/build.gradle.kts dependencies and Sync:
//      implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10")
//      ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
//      ksp { arg("appfunctions.aggregateAppFunctions", "true") } // if necessary
// 3. Uncomment block comments in this file and enable the imports/annotations below
// 4. Create res/xml/app_metadata.xml and register service/app_metadata in AndroidManifest.xml
// 5. Run ./gradlew :app:assembleDebug on JVM17 -> verify generated files in app/build/generated/ksp/debug/assets/*.xml
// 6. Verify registration with adb shell cmd app_function list-app-functions (docs/appfunctions-discovery.md Chapter 4)
// ---------------------------------------------------------------------------

// --- Imports to uncomment during activation ---
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
 * The implementation body is kept as a template in the block comments below.
 * Uncomment when enabling.
 */
object ScanCardAppFunctionServicePlaceholder {
    const val SERVICE_NAME = "ScanCardAppFunctionService"
    const val XML_FILE_NAME = "scan_card_app_function_service"
    const val PACKAGE_NAME = "com.plath.scancard"
    const val FUNCTION_IDS = "createDeck, scanAndExtract, searchCards, exportDeck"
}

/*
 * ===========================================================================
 * Implementation template to be uncommented upon activation (Hilt + ServiceEntryPoint pattern)
 * ===========================================================================
 * // res/xml/app_metadata.xml example:
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
 * // AndroidManifest.xml registration example (within <application>):
 * // <service
 * //     android:name="com.plath.scancard.service.ScanCardAppFunctionService"
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
 *     // Hilt injection — reuse existing UseCase/Repository to avoid redundant abstractions
 *     @Inject internal lateinit var manageDeckUseCase: com.plath.scancard.domain.usecase.ManageDeckUseCase
 *     @Inject internal lateinit var extractCardsUseCase: com.plath.scancard.domain.usecase.ExtractCardsUseCase
 *     @Inject internal lateinit var studyCardsUseCase: com.plath.scancard.domain.usecase.StudyCardsUseCase
 *     @Inject internal lateinit var exportDataUseCase: com.plath.scancard.domain.usecase.ExportDataUseCase
 *     // Inject ScanRepository / ModelRepository as needed
 *
 *     // -----------------------------------------------------------------------
 *     // Serializable definition — inline KDoc mandatory (KSP ignores class-level @param)
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
 *         /** Japanese translation. May be empty. Example: "apple" (translated). */
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
 *     // AppFunctions — KDoc optimized (docs/appfunctions-discovery.md Chapter 3)
 *     // All functions are suspend + withContext(Dispatchers.IO) to avoid blocking the UI thread
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
 *         // imageUri permission check — SecurityException is mapped to InvalidArgument
 *         // Note: Actual OCR->Gemma extraction is delegated to ExtractCardsUseCase.extractAndSaveCards(deckId, modelConfig).
 *         //       This template assumes synchronous calls of scanRepository.getScansByDeck + Gemma,
 *         //       but direct extraction pipeline from image URI will be extended in the future (extract after ScanRepository.insertScan).
 *         try {
 *             // Example: extractCardsUseCase.extractAndSaveCards(deckId, ModelConfig(modelConfig ?: "default"))
 *             // After success, retrieve extraction results with studyCardsUseCase.getCards(deckId).first()
 *         } catch (e: SecurityException) {
 *             throw AppFunctionInvalidArgumentException("Cannot read imageUri: $imageUri — permission denied")
 *         } catch (e: IllegalStateException) {
 *             throw AppFunctionExecutionException("Model not found: $modelConfig — please download it first")
 *         }
 *         // Dummy return — map cards obtained above to CardSummary when implementing
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
 *         // Implementation: studyCardsUseCase.getCards(deckId).first()
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
 * // Destructive action confirmation dialog procedure (Security constraints)
 * // ---------------------------------------------------------------------------
 * // deleteDeck / deleteCard are irreversible and will not be exposed as AppFunctions in this IMP.
 * // Safe pattern for exposure:
 * // 1. AppFunction returns a PendingIntent instead of deleting directly:
 * //      @AppFunction fun requestDeleteDeck(deckId: Long): PendingIntent
 * //      -> PendingIntent wraps an Intent that launches a confirmation dialog (AlertDialog) in MainActivity
 * // 2. ManageDeckUseCase.deleteDeck is executed only after the user taps "Delete" in the dialog
 * // 3. Inform the Agent: "User confirmation required — please launch pendingIntent to show the confirmation dialog"
 * //    via KDoc/throws
 * // 4. Alternative: Request confirmation via AppFunctionExecutionException before deletion within AppFunction,
 * //    and require a confirmed=true parameter in the second call
 * // Both follow "No destructive execution without confirmation" (skill Critical constraints: Security)
 * //
 * // Example (for reference when enabling):
 * // @AppFunction(isDescribedByKDoc = true)
 * // suspend fun requestDeleteDeck(deckId: Long): PendingIntent = withContext(Dispatchers.IO) {
 * //     val deck = manageDeckUseCase.getDeck(deckId) ?: throw AppFunctionElementNotFoundException(...)
 * //     // Return confirmation dialog Intent as PendingIntent
 * //     // Intent(context, MainActivity::class.java).apply { action="confirm_delete_deck"; putExtra("deckId", deckId) }
 * // }
 *
 */

# Requirements Document

## Introduction

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document correction, and extract flashcard term/definition pairs using the on-device LLM (Gemma 4 E2B) - all in fully offline mode after initial setup via Google Play.

## Glossary

- **ScanCard**: The Android application being developed
- **Gemma 4**: On-device LLM model (E2B only) for flashcard extraction.
- **Play for On-device AI**: Google's official infrastructure for delivering machine learning models on-demand via the Play Store.
- **AI Pack**: A downloadable bundle containing AI models, delivered securely through Google Play.
- **Deck**: A collection of flashcards organized by book title or chapter.
- **Card**: A flashcard containing a term (front) and definition (back).
- **OCR**: Optical Character Recognition for text extraction from images.
- **ML Kit**: Google ML Kit for document scanning and text recognition.
- **LiteRT LM**: Google's framework for on-device LLM inference (formerly MediaPipe GenAI).
- **OOM**: Out of Memory - application crash due to insufficient memory.
- **E2B Translation**: English-to-Bilingual (Japanese and English) translation provided by the on-device LLM.
- **Card Status**: The learning state of a card (NEW, LEARNING, REVIEW).
- **Background Task**: A long-running operation that continues execution while the user interacts with other UI elements.
- **TSV**: Tab-Separated Values format for card export.

## Hardware Requirements

- **Minimum RAM**: 4GB (E2B).
- **Storage**: Minimum 3GB free space for the E2B model.
- **Android Version**: API 26 (Android 8.0) or higher.

---

## Requirements

### Requirement 4: AI Model Selection and Management (Google Play)

**User Story:** As a user, I want to choose the AI model that fits my device's performance, so that extraction works efficiently without managing external tokens.

#### Acceptance Criteria

1. THE ScanCard SHALL provide the Gemma 4 E2B model for extraction.
2. THE ScanCard SHALL display the name, description, and estimated storage size of each model.
3. THE ScanCard SHALL use **Google Play AI Delivery** to download and manage model files (AI Packs) on-demand.
4. THE ScanCard SHALL NOT require external authentication (like Hugging Face PAT) for model access, relying instead on system-level delivery via the Play Store.
5. WHILE a download is in progress, THE ScanCard SHALL display a percentage-based progress indicator provided by the AiPackManager.

---

### Requirement 5: Automatic Flashcard Extraction

**User Story:** As a user, I want the AI to automatically extract key terms and definitions from scanned text.

#### Acceptance Criteria

1. WHEN OCR text is available and a model is ready (COMPLETED status), THE GemmaCardExtractor SHALL send the combined text to the selected LiteRT LM model.
... (Rest same as before)

---

### Requirement 6: Duplicate Card Prevention

**User Story:** As a user, I want the system to prevent duplicate terms from being added to my deck, so that I can avoid redundant study materials.

#### Acceptance Criteria

1. WHEN a new card is extracted, THE CardValidator SHALL check for existing terms with identical text in the same deck.
2. IF a duplicate term is found, THEN THE ScanCard SHALL display a warning to the user with the option to keep or skip the duplicate.
3. WHEN the user chooses to keep the duplicate, THE ScanCard SHALL add the card with a visual indicator distinguishing it from the original.
4. IF multiple cards with the same term have different definitions, THEN THE ScanCard SHALL display both definitions together for comparison.

---

### Requirement 7: Simplified Export Function

**User Story:** As a user, I want to copy my flashcards to the clipboard in a simple format, so that I can paste them anywhere without Quizlet-specific formatting.

#### Acceptance Criteria

1. WHEN the user requests export, THE ExportManager SHALL generate cards in TSV format (Term[TAB]Definition).
2. THE ExportManager SHALL NOT include "(Quizlet)" labels in the exported content.
3. WHEN export is complete, THE ScanCard SHALL copy the TSV content to the system clipboard.
4. THE ScanCard SHALL display a success message confirming the clipboard copy.

---

### Requirement 8: Card List Editing

**User Story:** As a user, I want to edit, add, and delete cards in my deck, so that I can maintain accurate and complete study materials.

#### Acceptance Criteria

1. THE CardListScreen SHALL provide an edit button for each card.
2. WHEN the edit button is pressed, THE CardEditor SHALL allow modification of the term and definition fields.
3. THE CardListScreen SHALL provide an add button to create new cards.
4. WHEN the add button is pressed, THE CardEditor SHALL offer two options: capture via camera or enter manually.
5. THE CardListScreen SHALL provide a delete button for each card.
6. WHEN the delete button is pressed, THE ScanCard SHALL confirm the deletion before removing the card.
7. THE CardListScreen SHALL update immediately after any edit, add, or delete operation.

---

### Requirement 9: Study Screen Filter Enhancement

**User Story:** As a user, I want to filter study cards by their learning status, so that I can focus on specific types of cards.

#### Acceptance Criteria

1. THE StudyScreen SHALL provide a filter dropdown with the following options: ALL, NEW, LEARNING, REVIEW.
2. WHEN "NEW" is selected, THE CardFilter SHALL display only cards with NEW status.
3. WHEN "LEARNING" is selected, THE CardFilter SHALL display only cards with LEARNING status.
4. WHEN "REVIEW" is selected, THE CardFilter SHALL display only cards with REVIEW status.
5. WHEN "ALL" is selected, THE CardFilter SHALL display all cards regardless of status.
6. THE CardFilter SHALL preserve the selected filter across StudyScreen sessions.

---

### Requirement 10: Bilingual Card Display

**User Story:** As a user, I want to see both English and Japanese definitions on the card back, so that I can study with translation support.

#### Acceptance Criteria

1. THE CardBack SHALL display the English definition by default.
2. THE CardBack SHALL provide a toggle button labeled "Japanese" to switch to Japanese.
3. WHEN the toggle button is pressed, THE CardDisplay SHALL show the Japanese translation of the definition.
4. THE CardBack SHALL preserve the E2B bilingual translation (English definition with Japanese annotation) alongside the toggle feature.
5. WHILE the toggle is active, THE CardDisplay SHALL maintain the selected language preference until changed by the user.

---

### Requirement 11: Translation Prompt Improvement

**User Story:** As a user, I want the AI to generate accurate translations that reflect the actual term content, so that my flashcards contain meaningful study materials.

#### Acceptance Criteria

1. THE TranslationPromptBuilder SHALL ensure that the term content appears verbatim in the generated definition.
2. IF the generated definition does not contain the original term or produces a generic response like "A topic to Explore", THEN THE PromptValidator SHALL flag this as an invalid translation.
3. WHEN an invalid translation is detected, THE ScanCard SHALL retry the extraction with an improved prompt.
4. THE improved prompt SHALL include explicit instructions to use the exact term in the definition.

---

### Requirement 12: Background Processing for Card Extraction

**User Story:** As a user, I want the card extraction to continue in the background, so that I can continue using the app while processing completes.

#### Acceptance Criteria

1. WHEN extraction is initiated, THE BackgroundTaskManager SHALL register it as a long-running task.
2. WHILE extraction is in progress, THE ScanCard SHALL display a progress indicator in the UI.
3. THE BackgroundTaskManager SHALL allow the user to navigate away from the extraction screen without cancelling the operation.
4. WHEN extraction completes in the background, THE BackgroundTaskManager SHALL notify the user via a system notification.
5. IF the app is terminated while extraction is in progress, THE BackgroundTaskManager SHALL resume the task when the app restarts.
6. THE BackgroundTaskManager SHALL run extraction as a WorkManager foreground service with `foregroundServiceType="dataSync"` declared on `SystemForegroundService` (targetSDK 36) and a non-null `ForegroundInfo` to prevent SystemJobService `onStopJob` cancellation after ~10s. The `dataSync` type SHALL be used instead of `shortService` because `shortService` imposes a hard ~3 minute limit that kills real multi-minute LLM extraction mid-run.
7. ANY E2E test helper that bypasses camera/LLM (e.g., "Insert Dummy Scan (E2E)", "Create Dummy Model (E2E)") SHALL be gated by `BuildConfig.DEBUG` and SHALL NOT be visible or reachable in release builds. The helpers SHALL insert a dummy scan and a <5MB dummy model file to exercise the foreground pipeline without requiring 2.6GB assets or GMS scanner.
8. WHEN extraction succeeds, THE ExtractCardsUseCase SHALL persist extracted cards via `cardRepository.insertCards` AND set the deck's durable `extractionStatus = COMPLETED` in Room BEFORE the worker reports `Result.success()`. Extraction results SHALL survive app restarts; reopening the app SHALL NOT re-run extraction for decks whose cards are already persisted.
9. WHEN extraction starts, THE deck's `extractionStatus` SHALL transition NONE → PENDING (at enqueue time) → RUNNING (at worker start). IF the worker fails permanently (retries exhausted), THEN the status SHALL be FAILED. IF the process dies or the job is cancelled by the system mid-run, THEN the status MAY remain RUNNING/PENDING until resume logic reconciles it.
10. WHEN the app launches, THE ScanCardApplication SHALL scan for decks stuck in PENDING or RUNNING status and re-enqueue extraction for any stuck deck that has no RUNNING/ENQUEUED WorkManager work. Decks with BLOCKED/CANCELLED/FAILED/no work SHALL be force-resumed with `ExistingWorkPolicy.REPLACE` to clear stale BLOCKED chains; decks with active RUNNING/ENQUEUED work SHALL be skipped (auto-resume).
11. THE BackgroundTaskManager SHALL enqueue unique work with `ExistingWorkPolicy.APPEND_OR_REPLACE` for normal `startExtraction` (preserves live RUNNING) and `ExistingWorkPolicy.REPLACE` for `resumeExtraction` (clears BLOCKED chains), both with `BackoffPolicy.EXPONENTIAL 10s` so that previously CANCELLED/FAILED runs do not silently block or get dropped on retry.
12. WHILE extraction is running, THE ScanCard SHALL post an ongoing progress notification in the notification area showing how many of the uploaded pages have been processed (`Page n of m`) with a determinate progress bar. The progress notification SHALL be posted under an app-managed notification id (`deckId + 100_000`, distinct from WorkManager's FGS notification id) because same-id updates are overwritten by WorkManager's automatic FGS re-post on every `setProgress` call; it SHALL NOT alert more than once and SHALL be replaced by the completion or error notification (same app-managed id) when extraction finishes.
13. THE progress counter SHALL reflect real work: `ExtractCardsUseCase` SHALL process uploaded scans page-by-page (one LLM call per page) and report `(0, N)` before the first page and `(i, N)` immediately after page i finishes, WHERE N is the number of uploaded scans for the deck.
14. THE worker SHALL additionally expose progress via WorkManager `setProgress` (`progress_current`, `progress_total`) so that in-app UI can observe the same progress without reading notifications.

### Requirement 18: Fast Extraction Flow (Performance Optimization)

**User Story:** As a user, I want scanning and extraction to complete as quickly as possible, so that I can start studying without unnecessary waiting or manual steps.

#### Acceptance Criteria

1. WHEN multiple pages are scanned, THE ScanDocumentUseCase SHALL process OCR for all pages in parallel using `coroutineScope` + `async`/`awaitAll` (one `TextRecognitionManager.recognizeText` per page concurrently) and preserve page order when inserting scans.
2. THE TextRecognitionManager.recognizeText SHALL execute `InputImage.fromFilePath` inside `withContext(Dispatchers.IO)` to avoid blocking the Main thread during OCR initialization.
3. WHEN OCR completes and a model is in `Ready` state (checked via `ModelRepository.checkModelStatus(GEMMA_4_E2B)` / `modelState`), THE ScanViewModel SHALL automatically trigger `BackgroundTaskManager.startExtraction` with `ModelConfig.DEFAULT_ID` and report `fastMode=true` so that `ScanCardNavHost` navigates directly to `DeckDetail` (skipping `ExtractionPreviewScreen`).
4. WHEN OCR completes and no model is `Ready`, THE ScanCard SHALL navigate to `ExtractionPreviewScreen` as before (fallback path).
5. WHILE auto-triggered extraction is running, THE ScanCard SHALL show the same ongoing progress notification (`Page n of m`, Req 12.12) and replace it with the completion notification on success (Req 12.4).

#### Notes
- The dummy scan/model path is test-only: `ScanDocumentUseCase.insertDummyScan()` and `ExtractionPreviewScreen` create `files/gemma-4-E2B-it.litertlm` with <5MB, triggering `GemmaCardExtractor` dummy mode (1s delay, 2 cards Apple/Banana) only when `BuildConfig.DEBUG` and file size <5MB.
- Durable state model: `Deck.extractionStatus: ExtractionStatus` (`NONE, PENDING, RUNNING, COMPLETED, FAILED`) stored in Room. This is the single source of truth for "was this deck already extracted and saved" — WorkManager state alone is not durable enough (terminal CANCELLED/FAILED states are never auto-retried).
- Fast Mode is verified by unit tests (parallel OCR timing + auto-trigger branching) and by Appium E2E `fastFlow.e2e.js` (fallback + `@slow` auto-extraction path via DEBUG dummy model file).

---

### Requirement 19: Room Persistence After App Kill

**User Story:** As a user, I want decks, scans, and cards to remain visible after the app is killed and restarted.

#### Acceptance Criteria

1. WHEN the app is killed (process death via `am force-stop` or swipe) and relaunched, THEN all `Deck`, `Scan`, `Card` rows persisted in `scancard_db` SHALL be visible via `DeckDao.getAllDecks()` / `CardDao.getCardsByDeck()` without being cleared. Cards that were displayed once (manual Add or dummy extraction) SHALL NOT disappear.
2. THE AppDatabase SHALL be built with `exportSchema=true`, version `5`, and `MIGRATION_2_3` (`isDuplicate`, `duplicateOfId`) + `MIGRATION_3_4` + `MIGRATION_4_5` (idempotent repair for v4 DBs missing columns) via `addMigrations()`, `setJournalMode(TRUNCATE)` to avoid WAL loss on force-stop. `fallbackToDestructiveMigration()` SHALL NOT be used.
3. THE Room `schemaDirectory("$projectDir/schemas")` SHALL be kept and `schemas/com.plath.scancard.data.local.AppDatabase/5.json` checked in.

---

### Requirement 20: Study Status Persistence and Visual Mark

**User Story:** As a user, I want my Learn/Review choices to be saved and visible as marks in the deck list.

#### Acceptance Criteria

1. WHEN the user taps **Learned** on a Study card, THEN `StudyCardsUseCase.markAsLearned` SHALL update `cards.status = LEARNING` via `CardDao.updateCardStatus`; WHEN tapping **Need Review**, THEN status SHALL become `REVIEW`. The update SHALL survive process death and be observable via `getCardsByDeck()` Flow.
2. `DeckDetailScreen.CardListItem` SHALL display a status badge: `NEW` is hidden, `LEARNING`/`REVIEW` SHALL show an `AssistChip` and a trailing label with `testTag="cardStatus_<id>_<STATUS>"` and `cardStatusLabel_<id>`.
3. `StudyScreen` SHALL display the current card's status in an `AssistChip` with `testTag="studyStatus_<STATUS>"` so that E2E can assert persistence without reading the database directly.

---

### Requirement 21: Study Completion Navigation

**User Story:** As a user, when I finish reviewing the last card in Study, I want to return automatically to the deck list.

#### Acceptance Criteria

1. `StudyViewModel` SHALL expose `isComplete: StateFlow<Boolean>` that becomes `true` when the user reviews the last card (`currentIndex == size-1` before mark) or calls `nextCard()` on the last card. `consumeComplete()` SHALL reset it to `false`.
2. WHEN `isComplete` becomes `true`, THEN `StudyScreen` SHALL call `onBack()` via `LaunchedEffect`, popping back to `DeckDetailScreen` (the deck list). On filtered views (`FilterType != ALL`), non-last reviews SHALL NOT advance the index (the filtered card leaves the list and the next card slides into place); on `ALL` filter, non-last reviews SHALL advance via `nextCard()`.
3. THE navigation graph SHALL provide `StudyScreen(onBack = { navController.popBackStack() })` so that completion returns to the caller without creating a new back-stack entry.

---

### Requirement 22: Scan Capture Post-Photo Visibility (Camera Dark Screen Fix)

**User Story:** As a user, after I take a photo with the camera scanner, I want the scanned pages to be visible instead of a black screen.

#### Acceptance Criteria

1. WHEN `GmsDocumentScanning` returns `RESULT_OK` with pages, THEN `ScanScreen` SHALL call `viewModel.addPages(uris)` and display the `LazyVerticalGrid` with thumbnails (`AsyncImage` via `ImageRequest` + `crossfade`) and the `Extract Cards (N)` bottom bar; the screen SHALL NOT remain black/dark.
2. WHEN the scanner returns `RESULT_CANCELED` or `null`/empty pages, THEN `ScanScreen` SHALL NOT set `isProcessing=true` and SHALL show the empty state with `Start Scanning` and, if `scannerError` is set, a retry `Card` with `testTag="scanRetryBtn"` and `Snackbar` — never a silent black screen.
3. WHEN `getStartScanIntent` fails, THEN `ScanScreen` SHALL set `scannerError="Scanner unavailable: …"` and surface it via `SnackbarHost` + retry card, allowing gallery import or manual retry.
4. `alreadyAutoLaunched` SHALL be `rememberSaveable` so that rotation/config change does not re-launch the scanner while the previous overlay is dimming (which was perceived as black). `MainActivity` SHALL wrap content in `ScanCardTheme` and `Scaffold(containerColor=background)` with `Surface` background to avoid `DayNight` windowBackground mismatch.
5. `AsyncImage` for each `uri` SHALL have `onError` logging and a `surfaceVariant` background + `Page` label fallback so that a failed load is visible as a placeholder rather than a black cell.

---

### Requirement 23: Direct Camera Launch from Top (Skip Start Scanning Screen)

**User Story:** As a user, I want to tap Scan on the top screen and immediately enter the camera shooting state without an extra "Start Scanning" screen.

#### Acceptance Criteria

1. WHEN the user taps `HomeScreen` FAB `homeFabScan` or `DeckDetailScreen` "Scan Document", THEN `ScanScreen` SHALL directly launch `GmsDocumentScanning.getStartScanIntent` without requiring a second tap on "Start Scanning".
2. WHEN `ScanScreen` is entered with `scannedPages.isEmpty()` and `hasCameraPermission=true` and `!isProcessing`, THEN it SHALL show a loading indicator `CircularProgressIndicator` + text "Opening camera..." (testTag `scanOpeningIndicator`) while the scanner Intent is being obtained, NOT a primary `Start Scanning` button. The `Start Scanning` button (`scanStartBtn`) SHALL only appear as fallback when `scannerError != null` or scanner was `RESULT_CANCELED`.
3. THE auto-launch SHALL be guarded by `rememberSaveable alreadyAutoLaunched` so that rotation/config change does not re-launch while the previous overlay is dimming (prevents black screen). It SHALL trigger exactly once per fresh entry.
4. IF `hasCameraPermission=false`, THEN permission request SHALL be shown first; immediately after grant, the scanner SHALL auto-launch without extra tap.
5. E2E SHALL verify: `homePage.tapFabScan()` → no `scanStartBtn` required; scanner overlay appears (GMS package) or `ScanScreen` shows `scanOpeningIndicator`; after dismissing GMS, dummy insertion `scanDummyInsertBtn` is ready without tapping Start Scanning.

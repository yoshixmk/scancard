# Requirements Document

## Introduction

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document correction, and extract flashcard term/definition pairs using on-device LLMs (Gemma 4 family) - all in fully offline mode after initial setup via Google Play.

## Glossary

- **ScanCard**: The Android application being developed
- **Gemma 4**: On-device LLM model family (E2B, E4B) for flashcard extraction.
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

- **Minimum RAM**: 4GB (E2B) / 8GB (E4B).
- **Storage**: Minimum 3GB - 6GB free space depending on selected model.
- **Android Version**: API 26 (Android 8.0) or higher.

---

## Requirements

### Requirement 4: AI Model Selection and Management (Google Play)

**User Story:** As a user, I want to choose the AI model that fits my device's performance, so that extraction works efficiently without managing external tokens.

#### Acceptance Criteria

1. THE ScanCard SHALL provide a selection of AI models from the Gemma 4 family (E2B, E4B).
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
2. THE CardBack SHALL provide a toggle button labeled "日本語" to switch to Japanese.
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
6. THE BackgroundTaskManager SHALL run extraction as a WorkManager foreground service with `foregroundServiceType="dataSync"` declared on `SystemForegroundService` (targetSDK 35) and a non-null `ForegroundInfo` to prevent SystemJobService `onStopJob` cancellation after ~10s. The `dataSync` type SHALL be used instead of `shortService` because `shortService` imposes a hard ~3 minute limit that kills real multi-minute LLM extraction mid-run.
7. ANY E2E test helper that bypasses camera/LLM (e.g., "Insert Dummy Scan (E2E)", "Create Dummy Model (E2E)") SHALL be gated by `BuildConfig.DEBUG` and SHALL NOT be visible or reachable in release builds. The helpers SHALL insert a dummy scan and a <5MB dummy model file to exercise the foreground pipeline without requiring 2.6GB assets or GMS scanner.
8. WHEN extraction succeeds, THE ExtractCardsUseCase SHALL persist extracted cards via `cardRepository.insertCards` AND set the deck's durable `extractionStatus = COMPLETED` in Room BEFORE the worker reports `Result.success()`. Extraction results SHALL survive app restarts; reopening the app SHALL NOT re-run extraction for decks whose cards are already persisted.
9. WHEN extraction starts, THE deck's `extractionStatus` SHALL transition NONE → PENDING (at enqueue time) → RUNNING (at worker start). IF the worker fails permanently (retries exhausted), THEN the status SHALL be FAILED. IF the process dies or the job is cancelled by the system mid-run, THEN the status MAY remain RUNNING/PENDING until resume logic reconciles it.
10. WHEN the app launches, THE ScanCardApplication SHALL scan for decks stuck in PENDING or RUNNING status and re-enqueue extraction for any of them that has no active (non-terminal) WorkManager work, restoring automatic resume after process death or system cancellation.
11. THE BackgroundTaskManager SHALL enqueue unique work with `ExistingWorkPolicy.APPEND_OR_REPLACE` and a backoff policy so that previously CANCELLED/FAILED runs do not silently block or get dropped on retry, WHILE never cancelling an already-RUNNING extraction.
12. WHILE extraction is running, THE ScanCard SHALL post an ongoing progress notification in the notification area showing how many of the uploaded pages have been processed (`Page n of m`) with a determinate progress bar. The progress notification SHALL be posted under an app-managed notification id (`deckId + 100_000`, distinct from WorkManager's FGS notification id) because same-id updates are overwritten by WorkManager's automatic FGS re-post on every `setProgress` call; it SHALL NOT alert more than once and SHALL be replaced by the completion or error notification (same app-managed id) when extraction finishes.
13. THE progress counter SHALL reflect real work: `ExtractCardsUseCase` SHALL process uploaded scans page-by-page (one LLM call per page) and report `(0, N)` before the first page and `(i, N)` immediately after page i finishes, WHERE N is the number of uploaded scans for the deck.
14. THE worker SHALL additionally expose progress via WorkManager `setProgress` (`progress_current`, `progress_total`) so that in-app UI can observe the same progress without reading notifications.

#### Notes
- The dummy scan/model path is test-only: `ScanDocumentUseCase.insertDummyScan()` and `ExtractionPreviewScreen` create `files/gemma-4-E2B-it.litertlm` with <5MB, triggering `GemmaCardExtractor` dummy mode (1s delay, 2 cards Apple/Banana) only when `BuildConfig.DEBUG` and file size <5MB.
- Durable state model: `Deck.extractionStatus: ExtractionStatus` (`NONE, PENDING, RUNNING, COMPLETED, FAILED`) stored in Room. This is the single source of truth for "was this deck already extracted and saved" — WorkManager state alone is not durable enough (terminal CANCELLED/FAILED states are never auto-retried).

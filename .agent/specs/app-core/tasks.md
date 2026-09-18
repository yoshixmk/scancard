# Implementation Plan: ScanCard Android Application

## Overview

This implementation plan covers the development of the ScanCard Android application, which enables users to photograph book pages, extract flashcard term/definition pairs using the on-device LLM (Gemma 4 E2B), and study using interactive 3D flashcard animations. The plan is organized into numbered tasks with clear dependencies to ensure smooth implementation progression.

The implementation follows a modular architecture with distinct layers: Data Layer (Room database), Domain Layer (Use Cases), Model Management (Google Play AI Delivery & LiteRT), and Presentation Layer (Jetpack Compose UI).

## Task Dependency Graph

The following dependency graph defines the execution order for all tasks. Tasks must be completed in the specified order to ensure all prerequisites are available.

### Core Implementation Order

1. **Foundation & Setup** (Section 1-7): Already completed - establishes core project structure, database, AI integration, and UI screens.

2. **Data Validation Layer**: Task 9.1 (CardValidator) → Task 9.2 (Integration into ExtractCardsUseCase)
   - CardValidator must be implemented before integrating duplicate detection into use cases

3. **Export System**: Task 10.1 (ExportManager) → Task 10.2 (Update ExportDataUseCase)
   - ExportManager is standalone and has no dependencies

4. **Card Editing**: Task 11.1 (CardEditor) → Task 11.2 (Update DeckDetailScreen)
   - CardEditor must be implemented before integrating into UI

5. **Study Filtering Enhancement**: Task 12.1 (FilterType enum) → Task 12.2 (CardFilter) → Task 12.3 (Update StudyViewModel)
   - Sequential dependency: enum defines types, filter implements logic, viewmodel uses filter

6. **Bilingual Display**: Task 13.1 (LanguagePreference enum) → Task 13.3 (Card entity update) → Task 13.2 (IBilingualCardDisplay) → Task 13.4 (Update Study Screen)
   - Entity update and enum must precede the interface implementation

7. **Translation Prompt System**: Task 14.1 (ITranslationPromptBuilder) → Task 14.2 (Remove PromptValidator) → Task 14.3 (Single-prompt ExtractCardsUseCase)
   - Translation prompt with JSON format example is used for one-shot extraction; no validator retry.

8. **Background Processing**: Task 15.1 (WorkManager dependency) → Task 15.2 (IBackgroundTaskManager) → Task 15.3 (CardExtractionWorker) → Task 15.4 (System notification) → Task 15.5 (Update Extraction Screen)
   - Sequential: dependency → manager → worker → notification → UI

### Cross-Task Dependencies Summary

| Task | Dependencies | Description |
|------|--------------|-------------|
| 9.2 | 9.1 | ExtractCardsUseCase needs CardValidator |
| 10.2 | 10.1 | ExportDataUseCase needs ExportManager |
| 11.2 | 11.1 | DeckDetailScreen needs CardEditor |
| 12.2 | 12.1 | CardFilter needs FilterType enum |
| 12.3 | 12.2 | StudyViewModel needs CardFilter |
| 13.2 | 13.1, 13.3 | IBilingualCardDisplay needs LanguagePreference and Card entity |
| 13.4 | 13.2, 13.3 | Study Screen needs bilingual interface and entity support |
| 14.3 | 14.1, 14.2 | ExtractCardsUseCase uses single-prompt builder without validator |
| 15.2 | 15.1 | BackgroundTaskManager needs WorkManager dependency |
| 15.3 | 15.2 | CardExtractionWorker uses BackgroundTaskManager |
| 15.4 | 15.3 | Notification triggered by worker completion |
| 15.5 | 15.2, 15.3, 15.4 | Extraction Screen needs all background processing components |

```json
{
  "tasks": [
    { "id": "1.1", "name": "Create Gradle wrapper and project structure", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.2", "name": "Create app module build configuration (AGP 9.4.0, Kotlin 2.4.10)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.3", "name": "Create AndroidManifest.xml and initial resources", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.4", "name": "Setup Hilt and Base Application class", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.5", "name": "Implement Edge-to-Edge support (Android 15+)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.1", "name": "Create Room database entities (Deck, Card, Scan)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.2", "name": "Create Room DAOs with Flow support", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.3", "name": "Create AppDatabase class and Hilt providers", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.4", "name": "Implement Repositories (Deck, Card, Scan)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.1", "name": "Setup :gemma-ai-pack + :gemma-ai-pack-2 split modules (1.5GB/pack limit) with com.android.ai-pack plugin", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.2", "name": "Integrate com.google.android.play:ai-delivery SDK", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.3", "name": "Implement ModelManager using AiPackManager with DEBUG build fallback", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.4", "name": "Create ModelConfig for Gemma 4 E2B only (single-model policy)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "4.1", "name": "Implement ML Kit Document Scanner integration", "status": "completed", "dependencies": [], "optional": false },
    { "id": "4.2", "name": "Implement ML Kit Text Recognition (Japanese/English)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "4.3", "name": "Implement GemmaCardExtractor (LiteRT LM Engine/Conversation)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "4.4", "name": "Implement CardResponseParser with multi-strategy fallback", "status": "completed", "dependencies": [], "optional": false },
    { "id": "5.1", "name": "Implement ScanDocumentUseCase (OCR pipeline)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "5.2", "name": "Implement ExtractCardsUseCase (AI pipeline with config support)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "5.3", "name": "Implement ManageDeckUseCase (CRUD)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "5.4", "name": "Implement StudyCardsUseCase (Learning logic)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "5.5", "name": "Implement ExportDataUseCase (TSV/CSV)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.1", "name": "Implement Home Screen (Deck list, manual creation)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.2", "name": "Implement Scan Screen (Camera integration, fixed bottom bar processing)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.3", "name": "Implement Extraction Preview Screen (Model selector, Play download UI, Scrollable)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.4", "name": "Implement Deck Detail Screen (Card list, ID display for debug)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.5", "name": "Implement Study Screen (3D flip card animation, filters)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "6.6", "name": "Implement Export Screen (Preview, Copy, Share)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "7.1", "name": "Setup Jetpack Navigation Compose with type-safe routes", "status": "completed", "dependencies": [], "optional": false },
    { "id": "7.2", "name": "Implement Navigation Graph (Scan -> Extraction -> Detail flow)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "8.1", "name": "Unit tests for CardResponseParser", "status": "completed", "dependencies": [], "optional": false },
    { "id": "8.2", "name": "Integration tests for full Scan-to-Card flow", "status": "completed", "dependencies": [], "optional": true },
    { "id": "8.3", "name": "Performance profiling for Gemma 4 models on target devices", "status": "completed", "dependencies": [], "optional": true },
    { "id": "8.4", "name": "Verification of Quizlet TSV format", "status": "completed", "dependencies": [], "optional": false },
    { "id": "9.1", "name": "Create CardValidator interface and implementation", "status": "completed", "dependencies": [], "optional": false },
    { "id": "9.2", "name": "Integrate CardValidator into ExtractCardsUseCase", "status": "completed", "dependencies": ["9.1"], "optional": false },
    { "id": "10.1", "name": "Create ExportManager interface and implementation", "status": "completed", "dependencies": [], "optional": false },
    { "id": "10.2", "name": "Update ExportDataUseCase to use ExportManager", "status": "completed", "dependencies": ["10.1"], "optional": false },
    { "id": "11.1", "name": "Create CardEditor component", "status": "completed", "dependencies": [], "optional": false },
    { "id": "11.2", "name": "Update DeckDetailScreen with CRUD UI", "status": "completed", "dependencies": ["11.1"], "optional": false },
    { "id": "12.1", "name": "Create FilterType enum (ALL, NEW, LEARNING, REVIEW)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "12.2", "name": "Create CardFilter implementation", "status": "completed", "dependencies": ["12.1"], "optional": false },
    { "id": "12.3", "name": "Update StudyViewModel to use new FilterType", "status": "completed", "dependencies": ["12.2"], "optional": false },
    { "id": "13.1", "name": "Create LanguagePreference enum (ENGLISH, JAPANESE)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "13.2", "name": "Create IBilingualCardDisplay interface and implementation", "status": "completed", "dependencies": ["13.1"], "optional": false },
    { "id": "13.3", "name": "Update Card entity with japaneseTranslation field", "status": "completed", "dependencies": [], "optional": false },
    { "id": "13.4", "name": "Update Study Screen UI with language toggle", "status": "completed", "dependencies": ["13.2", "13.3"], "optional": false },
    { "id": "14.1", "name": "Create ITranslationPromptBuilder interface and implementation", "status": "completed", "dependencies": [], "optional": false },
    { "id": "14.2", "name": "Remove PromptValidator (single-prompt extraction, JSON example one-shot)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "14.3", "name": "Single-prompt extraction in ExtractCardsUseCase (no retry loop, try/finally close)", "status": "completed", "dependencies": ["14.1", "14.2"], "optional": false },
    { "id": "15.1", "name": "Add WorkManager dependency to build.gradle.kts", "status": "completed", "dependencies": [], "optional": false },
    { "id": "15.2", "name": "Create IBackgroundTaskManager interface and implementation", "status": "completed", "dependencies": ["15.1"], "optional": false },
    { "id": "15.3", "name": "Create CardExtractionWorker", "status": "completed", "dependencies": ["15.2"], "optional": false },
    { "id": "15.4", "name": "Add system notification for task completion", "status": "completed", "dependencies": ["15.3"], "optional": false },
    { "id": "15.5", "name": "Update Extraction Screen with background processing UI", "status": "completed", "dependencies": ["15.2", "15.3", "15.4"], "optional": false },
    { "id": "15.6", "name": "Fix CardExtractionWorker foreground service for targetSDK 36", "status": "completed", "dependencies": ["15.3", "15.4"], "optional": false },
    { "id": "15.7", "name": "Add DEBUG-only E2E helpers (scanDummyInsertBtn, createDummyModelBtn, Gemma dummy mode)", "status": "completed", "dependencies": ["15.2"], "optional": false },
    { "id": "15.8", "name": "Add Appium E2E backgroundExtraction.e2e.js", "status": "completed", "dependencies": ["15.6", "15.7"], "optional": false },
    { "id": "16.1", "name": "Add Deck.extractionStatus + TypeConverter, version 3->4", "status": "completed", "dependencies": [], "optional": false },
    { "id": "16.2", "name": "Fix foreground dataSync (shortService cap)", "status": "completed", "dependencies": ["15.3"], "optional": false },
    { "id": "16.3", "name": "BackgroundTaskManager suspend enqueue with APPEND_OR_REPLACE", "status": "completed", "dependencies": ["15.2"], "optional": false },
    { "id": "16.4", "name": "CardExtractionWorker RUNNING + retry + FAILED", "status": "completed", "dependencies": ["15.3"], "optional": false },
    { "id": "16.5", "name": "ExtractCardsUseCase COMPLETED after insertCards", "status": "completed", "dependencies": [], "optional": false },
    { "id": "16.6", "name": "ResumePendingExtractionsUseCase from Application.onCreate", "status": "completed", "dependencies": ["16.1", "16.3"], "optional": false },
    { "id": "17.1", "name": "ExtractCardsUseCase page-by-page onProgress", "status": "completed", "dependencies": [], "optional": false },
    { "id": "17.2", "name": "NotificationHelper progress (deckId+100000)", "status": "completed", "dependencies": ["17.1"], "optional": false },
    { "id": "17.3", "name": "CardExtractionWorker onProgress + setProgress", "status": "completed", "dependencies": ["17.1"], "optional": false },
    { "id": "17.4", "name": "Gemma dummy delay 8s for shade check", "status": "completed", "dependencies": [], "optional": false },
    { "id": "17.5", "name": "E2E backgroundExtraction shade verification", "status": "completed", "dependencies": ["17.2", "17.3"], "optional": false },
    { "id": "18.1", "name": "TextRecognitionManager Dispatchers.IO", "status": "completed", "dependencies": [], "optional": false },
    { "id": "18.2", "name": "ScanDocumentUseCase parallel OCR", "status": "completed", "dependencies": ["18.1"], "optional": false },
    { "id": "18.3", "name": "ScanViewModel fastMode auto-extraction", "status": "completed", "dependencies": ["18.2"], "optional": false },
    { "id": "18.4", "name": "ScanScreen/NavHost fastMode routing", "status": "completed", "dependencies": ["18.3"], "optional": false },
    { "id": "18.5", "name": "Unit tests ScanDocumentUseCase/ScanViewModel", "status": "completed", "dependencies": ["18.2", "18.3"], "optional": false },
    { "id": "18.6", "name": "E2E fastFlow.e2e.js fallback + @slow", "status": "completed", "dependencies": ["18.4"], "optional": false },
    { "id": "19.1", "name": "AppDatabase v5 exportSchema + MIGRATION_2_3/3_4/4_5 TRUNCATE", "status": "completed", "dependencies": [], "optional": false },
    { "id": "19.2", "name": "DatabaseModule addMigrations + setJournalMode TRUNCATE", "status": "completed", "dependencies": ["19.1"], "optional": false },
    { "id": "19.3", "name": "E2E cardDisappearAfterClose (2 tests) + instrumented persistence test TDD", "status": "completed", "dependencies": ["19.1", "19.2"], "optional": false },
    { "id": "19.4", "name": "Remove fallbackToDestructiveMigration usage", "status": "completed", "dependencies": ["19.1"], "optional": false },
    { "id": "20.1", "name": "StudyViewModel markAsLearned/Review persistence", "status": "completed", "dependencies": [], "optional": false },
    { "id": "20.2", "name": "DeckDetail CardListItem status chip", "status": "completed", "dependencies": ["20.1"], "optional": false },
    { "id": "20.3", "name": "StudyScreen status chip + button tags", "status": "completed", "dependencies": ["20.1"], "optional": false },
    { "id": "21.1", "name": "StudyViewModel isComplete + consumeComplete", "status": "completed", "dependencies": ["20.1"], "optional": false },
    { "id": "21.2", "name": "StudyScreen LaunchedEffect onBack", "status": "completed", "dependencies": ["21.1"], "optional": false },
    { "id": "21.3", "name": "NavHost Study popBackStack wiring", "status": "completed", "dependencies": ["21.1"], "optional": false },
    { "id": "22.1", "name": "BackgroundTaskManager hasRunningOrEnqueued + REPLACE resume", "status": "completed", "dependencies": ["16.3"], "optional": false },
    { "id": "22.2", "name": "ResumePendingExtractionsUseCase BLOCKED fix", "status": "completed", "dependencies": ["22.1"], "optional": false },
    { "id": "22.3", "name": "E2E studyAndResume.e2e.js kill+resume verification", "status": "completed", "dependencies": ["20.2", "22.2"], "optional": false },
    { "id": "23.1", "name": "ScanScreen post-photo visibility (retry + saveable + background)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "23.2", "name": "ScanScreen thumbnails ImageRequest + MainActivity ScanCardTheme", "status": "completed", "dependencies": ["23.1"], "optional": false },
    { "id": "23.3", "name": "E2E scanPostPhoto.e2e.js grid and retry verification", "status": "completed", "dependencies": ["23.1"], "optional": false },
    { "id": "24.1", "name": "ScanScreen empty state scanOpeningIndicator not primary scanStartBtn", "status": "completed", "dependencies": [], "optional": false },
    { "id": "24.2", "name": "rememberSaveable alreadyAutoLaunched auto-launch without extra tap", "status": "completed", "dependencies": ["24.1"], "optional": false },
    { "id": "24.3", "name": "E2E directScanLaunch.e2e.js FAB direct camera", "status": "completed", "dependencies": ["24.1"], "optional": false },
    { "id": "24.4", "name": "DeckDetail manual re-extraction (retry button + ViewModel + Req12.15)", "status": "completed", "dependencies": ["16.3"], "optional": false }
  ],
  "waves": [
    {
      "name": "Foundation",
      "tasks": ["12.1", "12.2", "12.3"]
    },
    {
      "name": "Data Layer",
      "tasks": ["9.1", "10.1", "13.3"]
    },
    {
      "name": "Translation System",
      "tasks": ["14.1", "14.2", "14.3"]
    },
    {
      "name": "Background Processing",
      "tasks": ["15.1", "15.2", "15.3", "15.4", "15.5", "15.6", "15.7", "15.8"]
    },
    {
      "name": "UI Integration",
      "tasks": ["9.2", "10.2", "11.1", "11.2", "13.4"]
    }
  ]
}
```

## Notes

### Implementation Considerations

- All new features (Tasks 9-15) integrate with existing completed tasks from Sections 1-8
- Tasks marked with [ ] are pending implementation; tasks marked with [x] are completed
- Background processing (Task 15) uses WorkManager for reliability - tasks survive app termination
- Bilingual support requires database migration to add `japaneseTranslation` field to Card entity
- Translation uses single-prompt extraction (no retry loop); JSON format example ensures one-shot success

### Testing Requirements

- Unit tests required for all new interfaces and implementations
- Property-based tests validate universal properties across random inputs
- Integration tests verify end-to-end flows from scan to card persistence

### Code Organization

- New components follow existing patterns in the codebase
- Interfaces defined in domain layer, implementations in data layer
- UI components colocated with existing screen implementations

## Tasks

### 1. Project Setup and Configuration
- [x] 1.1 Create Gradle wrapper and project structure
- [x] 1.2 Create app module build configuration (AGP 9.4.0, Kotlin 2.4.10)
- [x] 1.3 Create AndroidManifest.xml and initial resources
- [x] 1.4 Setup Hilt and Base Application class
- [x] 1.5 Implement Edge-to-Edge support (Android 15+)

### 2. Data Layer - Local Persistence
- [x] 2.1 Create Room database entities (Deck, Card, Scan)
- [x] 2.2 Create Room DAOs with Flow support
- [x] 2.3 Create AppDatabase class and Hilt providers
- [x] 2.4 Implement Repositories (Deck, Card, Scan)

### 3. Model Management (Google Play AI Packs & Dev Fallback)

Authoritative AI-pack plan: `.agent/specs/ai-pack/tasks.md`. Entries below remain as the app-core execution record.
- [x] 3.1 Setup `:gemma-ai-pack` module with `com.android.ai-pack` plugin
- [x] 3.2 Integrate `com.google.android.play:ai-delivery` SDK
- [x] 3.3 Implement ModelManager using `AiPackManager` with DEBUG build fallback
- [x] 3.4 Create ModelConfig for Gemma 4 E2B only (single-model policy)

### 4. Integration - ML Kit and LiteRT
- [x] 4.1 Implement ML Kit Document Scanner integration
- [x] 4.2 Implement ML Kit Text Recognition (Japanese/English)
- [x] 4.3 Implement GemmaCardExtractor (LiteRT LM Engine/Conversation)
- [x] 4.4 Implement CardResponseParser with multi-strategy fallback

### 5. Domain Layer - Use Cases
- [x] 5.1 Implement ScanDocumentUseCase (OCR pipeline)
- [x] 5.2 Implement ExtractCardsUseCase (AI pipeline with config support)
- [x] 5.3 Implement ManageDeckUseCase (CRUD)
- [x] 5.4 Implement StudyCardsUseCase (Learning logic)
- [x] 5.5 Implement ExportDataUseCase (TSV/CSV)

### 6. Presentation Layer - UI Screens
- [x] 6.1 Implement Home Screen (Deck list, manual creation)
- [x] 6.2 Implement Scan Screen (Camera integration, fixed bottom bar processing)
- [x] 6.3 Implement Extraction Preview Screen (Model selector, Play download UI, Scrollable)
- [x] 6.4 Implement Deck Detail Screen (Card list, ID display for debug)
- [x] 6.5 Implement Study Screen (3D flip card animation, filters)
- [x] 6.6 Implement Export Screen (Preview, Copy, Share)

### 7. Navigation
- [x] 7.1 Setup Jetpack Navigation Compose with type-safe routes
- [x] 7.2 Implement Navigation Graph (Scan -> Extraction -> Detail flow)

### 8. Testing & Validation
- [x] 8.1 Unit tests for CardResponseParser
- [ ] 8.2 Integration tests for full Scan-to-Card flow
- [ ] 8.3 Performance profiling for Gemma 4 models on target devices
- [x] 8.4 Verification of Quizlet TSV format

---

## New Requirements Implementation Tasks

### 9. Duplicate Card Prevention (Requirement 6)
- [x] 9.1 Create CardValidator interface and implementation
- [x] 9.2 Integrate CardValidator into ExtractCardsUseCase

### 10. Simplified Export Function (Requirement 7)
- [x] 10.1 Create ExportManager interface and implementation
- [x] 10.2 Update ExportDataUseCase to use ExportManager

### 11. Card List Editing (Requirement 8)
- [x] 11.1 Create CardEditor component (Implemented as Dialogs in UI)
- [x] 11.2 Update DeckDetailScreen with CRUD UI

### 12. Study Screen Filter Enhancement (Requirement 9)
- [x] 12.1 Create FilterType enum (ALL, NEW, LEARNING, REVIEW)
- [x] 12.2 Create CardFilter implementation (Integrated into StudyViewModel)
- [x] 12.3 Update StudyViewModel to use new FilterType

### 13. Bilingual Card Display (Requirement 10)
- [x] 13.1 Create LanguagePreference enum (ENGLISH, JAPANESE)
- [x] 13.2 Create IBilingualCardDisplay interface (Handled in UI state)
- [x] 13.3 Update Card entity with japaneseTranslation field
- [x] 13.4 Update Study Screen UI with language toggle

### 14. Translation Prompt Improvement (Requirement 11)
- [x] 14.1 Create ITranslationPromptBuilder interface and implementation
- [x] 14.2 Remove PromptValidator (single-prompt extraction, JSON example one-shot)
- [x] 14.3 Single-prompt extraction in ExtractCardsUseCase (no retry loop, try/finally close)

### 15. Background Processing for Card Extraction (Requirement 12)
- [x] 15.1 Add WorkManager dependency to build.gradle.kts
- [x] 15.2 Create BackgroundTaskManager
- [x] 15.3 Create CardExtractionWorker (`getForegroundInfo()` + `setForeground()` at `doWork()` start, `ForegroundInfo` deckId)
- [x] 15.4 Add system notification for task completion (`NotificationHelper` foreground + completion, channel `extraction_channel`)
- [x] 15.5 Update Extraction Screen with background processing UI
- [x] 15.6 Fix foreground service for targetSDK 36: `AndroidManifest.xml` merge `<service SystemForegroundService foregroundServiceType="dataSync">`, `FOREGROUND_SERVICE_DATA_SYNC` permission, `CardExtractionWorker` `DATA_SYNC` on `UPSIDE_DOWN_CAKE+` (verified on emulator-5554 API 36, `backgroundExtraction.e2e.js` 43.8s PASS)
- [x] 15.7 DEBUG-only E2E helpers (NOT in release): `ScanScreen.kt:199,245` `scanDummyInsertBtn` + `ScanViewModel.insertDummyScanForE2E()` + `ScanDocumentUseCase.insertDummyScan()` + `ExtractionPreviewScreen` `createDummyModelBtn` (Idle+DEBUG) + `GemmaCardExtractor` dummy mode (<5MB → 1s 2 cards), all gated by `BuildConfig.DEBUG`
- [x] 15.8 Appium E2E `specs/backgroundExtraction.e2e.js`: GMS `Discard` dialog handling, `clearStateAndLaunch` grants `CAMERA`+`POST_NOTIFICATIONS`, verifies `2 Cards` and `Dummy mode enabled` with no `Work cancelled`

### 16. Durable Extraction Persistence & Resume (Requirement 12.8–12.11)

- [x] 16.1 Add `Deck.extractionStatus: ExtractionStatus` (`NONE, PENDING, RUNNING, COMPLETED, FAILED`) + Room TypeConverter, DB version 3→4, `DeckDao.updateExtractionStatus()` / `getStuckExtractionDecks()`, `DeckRepository` pass-throughs
- [x] 16.2 Fix foreground type `shortService` → `dataSync` (~3min hard cap killed real extraction): manifest service override + worker `FOREGROUND_SERVICE_TYPE_DATA_SYNC`
- [x] 16.3 `BackgroundTaskManager`: suspend enqueue with Flow-based active-work check, `ExistingWorkPolicy.APPEND_OR_REPLACE`, `BackoffPolicy.EXPONENTIAL 10s`, PENDING status at enqueue
- [x] 16.4 `CardExtractionWorker`: RUNNING status at start, rethrow `CancellationException` (no more swallow→failure), `Result.retry()` up to 3 attempts, FAILED on permanent failure
- [x] 16.5 `ExtractCardsUseCase`: set COMPLETED immediately after `insertCards` (before `Result.success()`)
- [x] 16.6 `ResumePendingExtractionsUseCase` + launch from `ScanCardApplication.onCreate`: reconcile stuck PENDING/RUNNING decks with WorkManager state and re-enqueue only decks without active work

### 17. Extraction Progress Notification (Requirement 12.12–12.14)

- [x] 17.1 `ExtractCardsUseCase`: process scans page-by-page (single prompt per page, no retry loop), add `onProgress: suspend (current: Int, total: Int) -> Unit` reporting `(0,N)` before first page and `(i,N)` after each page
- [x] 17.2 `NotificationHelper.showProgressNotification(deckId, current, total)`: ongoing determinate notification (`setProgress`, `setOnlyAlertOnce`) posted under an app-managed id (`deckId + 100_000`) — same-id updates get overwritten by WorkManager's FGS re-post on every `setProgress` (verified on emulator)
- [x] 17.3 `CardExtractionWorker`: pass an `onProgress` lambda that posts the progress notification and mirrors it via WorkManager `setProgress` (`progress_current`/`progress_total`)
- [x] 17.4 `GemmaCardExtractor` dummy mode delay 1s → 8s (E2E shade-check window despite clock skew)
- [x] 17.5 E2E `backgroundExtraction.e2e.js`: open the notification shade mid-extraction and assert `Page n of m` is visible; verify completion still reaches `2 Cards` + persistence across restart

### 18. Fast Extraction Flow (Requirement 18 — Performance)

- [x] 18.1 `TextRecognitionManager.recognizeText`: wrap `InputImage.fromFilePath` in `withContext(Dispatchers.IO)` (Main-thread I/O → ANR fix)
- [x] 18.2 `ScanDocumentUseCase.processScannedPages`: parallel OCR via `coroutineScope` + `async`/`awaitAll`, preserve page order on `insertScan`
- [x] 18.3 `ScanViewModel`: add `ModelRepository` + `BackgroundTaskManager` deps; `processScans`/`insertDummyScanForE2E` check `checkModelStatus(GEMMA_4_E2B)` → `Ready` triggers `startExtraction(DEFAULT_ID)` and returns `fastMode=true`, else `false`
- [x] 18.4 `ScanScreen` + `ScanCardNavHost`: change `onComplete` to `(Long, Boolean) -> Unit`; NavHost routes `fastMode=true` → `DeckDetail` (skip `ExtractionPreviewScreen`), `false` → `ExtractionPreviewScreen`
- [x] 18.5 Unit tests: `ScanDocumentUseCaseTest` (virtual-time parallel timing + order), `ScanViewModelTest` (Ready→auto-extract, Idle→fallback)
- [x] 18.6 E2E `specs/fastFlow.e2e.js`: fallback (=`Idle` → preview) + `@slow` auto-extraction path (DEBUG dummy model file `<5MB` ⇒ `Ready` ⇒ DeckDetail direct + progress notification still appears)

### 19. Room Persistence After Kill (Requirement 19)

- [x] 19.1 `AppDatabase`: `exportSchema=true`, version `5`, `MIGRATION_2_3` + `MIGRATION_3_4` + `MIGRATION_4_5` idempotent repair, `5.json` checked in, `TRUNCATE` journal to survive `force-stop` WAL loss
- [x] 19.2 `DatabaseModule`: `Room.databaseBuilder(...).addMigrations(...).setJournalMode(TRUNCATE).build()` — file DB, no fallback, survives kill
- [x] 19.3 E2E `cardDisappearAfterClose.e2e.js` (2 tests) + instrumented `CardPersistenceAfterCloseTest` (file DB close/reopen) via TDD — both green
- [x] 19.4 Remove `fallbackToDestructiveMigration()` usage; keep `schemaDirectory` for future AutoMigrations

### 20. Study Status Persistence and Visual Mark (Requirement 20)

- [x] 20.1 `StudyViewModel`: `markAsLearned` → `LEARNING` / `markAsReviewNeeded` → `REVIEW` via `CardDao.updateCardStatus`; update survives process death and is observed via Flow
- [x] 20.2 `DeckDetailScreen.CardListItem`: show status `AssistChip` + trailing label `cardStatus_<id>_<STATUS>` (hidden for NEW)
- [x] 20.3 `StudyScreen`: show `AssistChip` `studyStatus_<STATUS>` for current card; buttons use `testTag="studyLearnedBtn"` / `studyReviewBtn` and no longer call `nextCard()` manually

### 21. Study Completion Navigation (Requirement 21)

- [x] 21.1 `StudyViewModel`: add `isComplete: StateFlow<Boolean>` + `consumeComplete()`; `markAs...` and `nextCard()` set `isComplete=true` when last card reviewed, `LaunchedEffect` in `StudyScreen` calls `onBack()`
- [x] 21.2 `StudyScreen`: `LaunchedEffect(isComplete){ if(true) {consumeComplete(); onBack()} }` — returns to DeckDetail list automatically; filtered case keeps index, ALL case advances
- [x] 21.3 `ScanCardNavHost`: `StudyScreen(onBack={popBackStack()})` wiring verified via E2E

### 22. Background Resume Fix (Requirement 12.10–12.11)

- [x] 22.1 `BackgroundTaskManager`: add `hasRunningOrEnqueuedWork()` (checks `RUNNING`/`ENQUEUED` only) and `resumeExtraction` uses `REPLACE` to clear `BLOCKED` chains; `enqueue()` now takes `policy` param (default `APPEND_OR_REPLACE`)
- [x] 22.2 `ResumePendingExtractionsUseCase`: skip only `RUNNING`/`ENQUEUED`, force-resume `BLOCKED`/`CANCELLED`/`FAILED`/empty with `REPLACE` — fixes kill+restart resume deadlock
- [x] 22.3 E2E verification in `studyAndResume.e2e.js`: kill→restart still shows decks/cards and resume re-enqueues

### 23. Scan Capture Post-Photo Visibility Fix (Requirement 22)

- [x] 23.1 `ScanScreen`: handle `RESULT_OK`/empty/`RESULT_CANCELED`/failure with `scannerError` + `SnackbarHost` + `scanRetryBtn`, `alreadyAutoLaunched` via `rememberSaveable` to prevent rotation double-launch black overlay, `Scaffold(containerColor=background)` + `background()` on content/grid
- [x] 23.2 `ScanScreen` thumbnails: `AsyncImage(ImageRequest.Builder(uri).crossfade)` with `surfaceVariant` background, `Page` label fallback and `onError` log — never black cell; `MainActivity` wraps in `ScanCardTheme` for correct `DayNight` windowBackground
- [x] 23.3 E2E `scanPostPhoto.e2e.js`: dummy insert shows `Extract Cards (1)` and `scanThumb_*`, retry path shows `scanRetryBtn` when scanner unavailable

### 24. Direct Camera Launch from Top (Requirement 23 — Skip Start Scanning)

- [x] 24.1 `ScanScreen` empty state: show `scanOpeningIndicator` (CircularProgressIndicator + "Opening camera...") while `getStartScanIntent` is obtained, NOT primary `scanStartBtn`; button only appears as fallback when `scannerError` or `RESULT_CANCELED`
- [x] 24.2 `ScanScreen` auto-launch guarded by `rememberSaveable alreadyAutoLaunched` + `hasCameraPermission` + `scannedPages.isEmpty()`, permission grant immediately auto-launches without extra tap
- [x] 24.3 E2E `directScanLaunch.e2e.js`: `homeFabScan` tap shows `scanOpeningIndicator` or GMS overlay without requiring `scanStartBtn`, after GMS dismiss `scanDummyInsertBtn` ready
- [x] 24.4 `DeckDetailScreen`: while `RUNNING`/`PENDING`, show loading row below card count (`deckDetailExtractionLoading`, "Extracting cards…") with or without cards and hide retry button; `Retry extraction` (`deckDetailRetryExtractionBtn`) only when no cards and idle → `DeckDetailViewModel.retryExtraction()` → `startExtraction(deckId, DEFAULT_ID)`; idempotent re-run, never auto-runs on reopen (Req 12.15)

---

## Implementation Order

The tasks should be implemented in the following order for optimal dependency management:

1. **Foundation tasks**: 12.1 → 12.2 → 12.3 (Filter system)
2. **Data/Model tasks**: 9.1 → 10.1 → 13.3 (CardValidator, ExportManager, Card entity update)
3. **Translation tasks**: 14.1 → 14.2 → 14.3 (Prompt system)
4. **Background tasks**: 15.1 → 15.2 → 15.3 → 15.4 → 15.5 (Background processing)
5. **UI integration tasks**: 9.2 → 10.2 → 11.1 → 11.2 → 13.4 (UI updates)
6. **Testing**: Remaining tests from Section 8

---

## Completed Milestones

- Migrated model delivery to **Google Play AI Delivery (AI Packs)**.
- Implemented **LiteRT LM SDK** for on-device inference with Universal CPU support.
- Fully implemented Edge-to-Edge support for Android 15.
- Core Scan-to-Study flow is fully functional and verified.
- **Improved Card Management**: Added CRUD operations and duplicate prevention.
- **Enhanced Study Experience**: Added bilingual toggle and NEW filter.
- **Background Processing**: Integrated WorkManager for long-running extractions.
- **Prompt Optimization**: Refined AI prompts for better E2B translations.

# Implementation Plan: ScanCard Android Application

## Overview

This implementation plan covers the development of the ScanCard Android application, which enables users to photograph book pages, extract flashcard term/definition pairs using on-device LLMs (Gemma 4 family), and study using interactive 3D flashcard animations. The plan is organized into numbered tasks with clear dependencies to ensure smooth implementation progression.

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

7. **Translation Prompt System**: Task 14.1 (ITranslationPromptBuilder) → Task 14.2 (IPromptValidator) → Task 14.3 (Integrate into ExtractCardsUseCase)
   - Both prompt builder and validator needed before use case integration

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
| 14.3 | 14.1, 14.2 | ExtractCardsUseCase needs prompt builder and validator |
| 15.2 | 15.1 | BackgroundTaskManager needs WorkManager dependency |
| 15.3 | 15.2 | CardExtractionWorker uses BackgroundTaskManager |
| 15.4 | 15.3 | Notification triggered by worker completion |
| 15.5 | 15.2, 15.3, 15.4 | Extraction Screen needs all background processing components |

```json
{
  "tasks": [
    { "id": "1.1", "name": "Create Gradle wrapper and project structure", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.2", "name": "Create app module build configuration (AGP 9.3, Kotlin 2.2)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.3", "name": "Create AndroidManifest.xml and initial resources", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.4", "name": "Setup Hilt and Base Application class", "status": "completed", "dependencies": [], "optional": false },
    { "id": "1.5", "name": "Implement Edge-to-Edge support (Android 15+)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.1", "name": "Create Room database entities (Deck, Card, Scan)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.2", "name": "Create Room DAOs with Flow support", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.3", "name": "Create AppDatabase class and Hilt providers", "status": "completed", "dependencies": [], "optional": false },
    { "id": "2.4", "name": "Implement Repositories (Deck, Card, Scan)", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.1", "name": "Setup :gemma-ai-pack module with com.android.ai-pack plugin", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.2", "name": "Integrate com.google.android.play:ai-delivery SDK", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.3", "name": "Implement ModelManager using AiPackManager with DEBUG build fallback", "status": "completed", "dependencies": [], "optional": false },
    { "id": "3.4", "name": "Create ModelConfig for Gemma 4 (Universal/CPU variants)", "status": "completed", "dependencies": [], "optional": false },
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
    { "id": "9.1", "name": "Create CardValidator interface and implementation", "status": "pending", "dependencies": [], "optional": false },
    { "id": "9.2", "name": "Integrate CardValidator into ExtractCardsUseCase", "status": "pending", "dependencies": ["9.1"], "optional": false },
    { "id": "10.1", "name": "Create ExportManager interface and implementation", "status": "pending", "dependencies": [], "optional": false },
    { "id": "10.2", "name": "Update ExportDataUseCase to use ExportManager", "status": "pending", "dependencies": ["10.1"], "optional": false },
    { "id": "11.1", "name": "Create CardEditor component", "status": "pending", "dependencies": [], "optional": false },
    { "id": "11.2", "name": "Update DeckDetailScreen with CRUD UI", "status": "pending", "dependencies": ["11.1"], "optional": false },
    { "id": "12.1", "name": "Create FilterType enum (ALL, NEW, LEARNING, REVIEW)", "status": "pending", "dependencies": [], "optional": false },
    { "id": "12.2", "name": "Create CardFilter implementation", "status": "pending", "dependencies": ["12.1"], "optional": false },
    { "id": "12.3", "name": "Update StudyViewModel to use new FilterType", "status": "pending", "dependencies": ["12.2"], "optional": false },
    { "id": "13.1", "name": "Create LanguagePreference enum (ENGLISH, JAPANESE)", "status": "pending", "dependencies": [], "optional": false },
    { "id": "13.2", "name": "Create IBilingualCardDisplay interface and implementation", "status": "pending", "dependencies": ["13.1"], "optional": false },
    { "id": "13.3", "name": "Update Card entity with japaneseTranslation field", "status": "pending", "dependencies": [], "optional": false },
    { "id": "13.4", "name": "Update Study Screen UI with language toggle", "status": "pending", "dependencies": ["13.2", "13.3"], "optional": false },
    { "id": "14.1", "name": "Create ITranslationPromptBuilder interface and implementation", "status": "pending", "dependencies": [], "optional": false },
    { "id": "14.2", "name": "Create IPromptValidator interface and implementation", "status": "pending", "dependencies": [], "optional": false },
    { "id": "14.3", "name": "Integrate PromptValidator into ExtractCardsUseCase", "status": "pending", "dependencies": ["14.1", "14.2"], "optional": false },
    { "id": "15.1", "name": "Add WorkManager dependency to build.gradle.kts", "status": "pending", "dependencies": [], "optional": false },
    { "id": "15.2", "name": "Create IBackgroundTaskManager interface and implementation", "status": "pending", "dependencies": ["15.1"], "optional": false },
    { "id": "15.3", "name": "Create CardExtractionWorker", "status": "pending", "dependencies": ["15.2"], "optional": false },
    { "id": "15.4", "name": "Add system notification for task completion", "status": "pending", "dependencies": ["15.3"], "optional": false },
    { "id": "15.5", "name": "Update Extraction Screen with background processing UI", "status": "pending", "dependencies": ["15.2", "15.3", "15.4"], "optional": false }
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
      "tasks": ["15.1", "15.2", "15.3", "15.4", "15.5"]
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
- Translation validation uses retry logic (up to 3 attempts) to improve AI output quality

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
- [x] 1.2 Create app module build configuration (AGP 9.3, Kotlin 2.2)
- [x] 1.3 Create AndroidManifest.xml and initial resources
- [x] 1.4 Setup Hilt and Base Application class
- [x] 1.5 Implement Edge-to-Edge support (Android 15+)

### 2. Data Layer - Local Persistence
- [x] 2.1 Create Room database entities (Deck, Card, Scan)
- [x] 2.2 Create Room DAOs with Flow support
- [x] 2.3 Create AppDatabase class and Hilt providers
- [x] 2.4 Implement Repositories (Deck, Card, Scan)

### 3. Model Management (Google Play AI Packs & Dev Fallback)
- [x] 3.1 Setup `:gemma-ai-pack` module with `com.android.ai-pack` plugin
- [x] 3.2 Integrate `com.google.android.play:ai-delivery` SDK
- [x] 3.3 Implement ModelManager using `AiPackManager` with DEBUG build fallback
- [x] 3.4 Create ModelConfig for Gemma 4 (Universal/CPU variants)

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
- [ ] 9.1 Create CardValidator interface and implementation
  - Implement duplicate detection with term normalization (trim, lowercase)
  - Provide DuplicateWarning and DuplicateAction enums
  - **Dependencies**: CardRepository (existing)
- [ ] 9.2 Integrate CardValidator into ExtractCardsUseCase
  - Check duplicates before card insertion
  - Handle SHOW_WARNING, AUTO_SKIP, AUTO_ADD_WITH_INDICATOR actions
  - **Dependencies**: Task 9.1

### 10. Simplified Export Function (Requirement 7)
- [ ] 10.1 Create ExportManager interface and implementation
  - Implement TSV format (Term[TAB]Definition) without Quizlet labels
  - Add proper escaping for special characters
  - **Dependencies**: None (standalone component)
- [ ] 10.2 Update ExportDataUseCase to use ExportManager
  - Add clipboard copy functionality
  - Show success message after export
  - **Dependencies**: Task 10.1

### 11. Card List Editing (Requirement 8)
- [ ] 11.1 Create CardEditor component
  - Implement edit, add, delete operations
  - Add confirmation dialog for delete
  - **Dependencies**: CardRepository (existing)
- [ ] 11.2 Update DeckDetailScreen with CRUD UI
  - Add edit button for each card
  - Add add button (camera/manual options)
  - Add delete button with confirmation
  - **Dependencies**: Task 11.1

### 12. Study Screen Filter Enhancement (Requirement 9)
- [ ] 12.1 Create FilterType enum (ALL, NEW, LEARNING, REVIEW)
  - Replace existing StudyFilter enum
  - **Dependencies**: None
- [ ] 12.2 Create CardFilter implementation
  - Implement filter by status
  - Support filter persistence
  - **Dependencies**: Task 12.1
- [ ] 12.3 Update StudyViewModel to use new FilterType
  - Use CardFilter for filtering logic
  - Persist filter selection
  - **Dependencies**: Task 12.2

### 13. Bilingual Card Display (Requirement 10)
- [ ] 13.1 Create LanguagePreference enum (ENGLISH, JAPANESE)
- [ ] 13.2 Create IBilingualCardDisplay interface and implementation
  - Manage language toggle state
  - Support session persistence
  - **Dependencies**: None
- [ ] 13.3 Update Card entity with japaneseTranslation field
  - Add field to Card entity
  - Update database schema
  - **Dependencies**: None
- [ ] 13.4 Update Study Screen UI with language toggle
  - Add toggle button "日本語" / "Switch to English"
  - Display correct translation based on preference
  - **Dependencies**: Task 13.2, 13.3

### 14. Translation Prompt Improvement (Requirement 11)
- [ ] 14.1 Create ITranslationPromptBuilder interface and implementation
  - Build standard prompts with term enforcement
  - Build improved prompts for retry scenarios
  - **Dependencies**: None
- [ ] 14.2 Create IPromptValidator interface and implementation
  - Validate term presence in definition (case-insensitive)
  - Detect generic responses like "A topic to Explore"
  - Return ValidationResult (Valid/Invalid)
  - **Dependencies**: None
- [ ] 14.3 Integrate PromptValidator into ExtractCardsUseCase
  - Add validation after AI extraction
  - Implement retry logic (up to 3 attempts)
  - **Dependencies**: Task 14.1, 14.2

### 15. Background Processing for Card Extraction (Requirement 12)
- [ ] 15.1 Add WorkManager dependency to build.gradle.kts
- [ ] 15.2 Create IBackgroundTaskManager interface and implementation
  - Enqueue extraction tasks with WorkManager
  - Track task status (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)
  - Support progress reporting
  - **Dependencies**: None
- [ ] 15.3 Create CardExtractionWorker
  - Implement long-running extraction logic
  - Report progress incrementally
  - Handle task resumption on app restart
  - **Dependencies**: Task 15.2
- [ ] 15.4 Add system notification for task completion
  - Show notification when extraction completes in background
  - Handle completion and failure states
  - **Dependencies**: Task 15.3
- [ ] 15.5 Update Extraction Screen with background processing UI
  - Show progress indicator during extraction
  - Allow navigation away without cancelling
  - **Dependencies**: Task 15.2, 15.3, 15.4

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
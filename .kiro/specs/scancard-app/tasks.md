# Implementation Plan: ScanCard Android Application

## Overview

This implementation plan covers the complete development of ScanCard, an Android application for batch document scanning, OCR text recognition, and AI-powered flashcard extraction using on-device Gemma Nano LLM. The app follows Clean Architecture with MVVM pattern, using Jetpack Compose for UI and Room Database for persistence.

## Tasks

### 1. Project Setup and Configuration

- [ ] 1.1 Create Gradle wrapper and project structure
  - Initialize Gradle wrapper with `gradle wrapper`
  - Create settings.gradle.kts and root build.gradle.kts
  - Configure Android Gradle Plugin version 8.x
  - _Requirements: 13.1, 13.2 (offline operation, no external data transmission)_

- [ ] 1.2 Create app module build configuration
  - Create app/build.gradle.kts with all dependencies
  - Add ML Kit Document Scanner, ML Kit Text Recognition
  - Add MediaPipe LLM Inference for Gemma Nano
  - Add Room Database, Hilt, Jetpack Compose dependencies
  - _Requirements: 14.2 (no analytics SDKs)_

- [ ] 1.3 Create AndroidManifest.xml
  - Declare CAMERA permission
  - Declare READ/WRITE_EXTERNAL_STORAGE for older APIs
  - Declare FOREGROUND_SERVICE for model downloads
  - Configure activities and intent filters

### 2. Data Layer - Database

- [ ] 2.1 Create Room database entities
  - [ ] 2.1.1 Create Deck entity with id, title, createdAt, updatedAt
  - [ ] 2.1.2 Create Card entity with id, deckId (FK), term, definition, isLearned, createdAt
  - [ ] 2.1.3 Create Scan entity with id, deckId (FK), imagePath, rawText
  - _Requirements: 15.1, 15.2, 15.3, 15.4_

- [ ] 2.2 Create Room DAOs
  - [ ] 2.2.1 Create DeckDao with getAll, getById, insert, update, delete
  - [ ] 2.2.2 Create CardDao with getByDeck, insert, update, delete, updateLearnedStatus
  - [ ] 2.2.3 Create ScanDao with getByDeck, insert, delete
  - _Requirements: 15.4, 15.5_

- [ ] 2.3 Create AppDatabase class
  - Define database with version and schema export
  - Configure foreign key constraints and migrations
  - _Requirements: 15.4, 15.5_

- [ ] 2.4 Create Repository implementations
  - [ ] 2.4.1 Create DeckRepository interface and implementation
  - [ ] 2.4.2 Create CardRepository interface and implementation
  - [ ] 2.4.3 Create ScanRepository interface and implementation
  - [ ] 2.4.4 Create ModelRepository for Gemma Nano model state
  - _Requirements: 8.1, 8.3, 8.4, 15.1, 15.2, 15.3_

### 3. Dependency Injection

- [ ] 3.1 Set up Hilt modules
  - [ ] 3.1.1 Create DatabaseModule for Room database and DAOs
  - [ ] 3.1.2 Create RepositoryModule for repository bindings
  - [ ] 3.1.3 Create AppModule for Context and application-level dependencies
  - _Requirements: All (DI is infrastructure)_

### 4. Domain Layer - Use Cases

- [ ] 4.1 Create domain models
  - ProcessedImage, OcrResult, CardPair, ModelState data classes

- [ ] 4.2 Implement ScanDocumentUseCase
  - Orchestrate scanning, image processing, and OCR
  - Handle sequential page processing with retry logic
  - _Requirements: 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 4.3 Implement ExtractCardsUseCase
  - Coordinate OCR text extraction and Gemma Nano inference
  - Handle parse fallback strategies
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [ ] 4.4 Implement ManageDeckUseCase
  - CRUD operations for decks with title validation (1-100 chars)
  - Cascade delete for cards and scans
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 4.5 Implement StudyCardsUseCase
  - Load cards, manage learned/review status
  - Implement spaced repetition prioritization
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 4.6 Implement ExportDataUseCase
  - Generate TSV/CSV exports for Quizlet
  - Generate searchable PDF
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 12.1, 12.2, 12.3, 12.4, 12.5_

### 5. Presentation Layer - ViewModels

- [ ] 5.1 Create HomeViewModel
  - Load decks sorted by updatedAt descending
  - Handle deck creation, editing, deletion
  - _Requirements: 8.2, 8.5_

- [ ] 5.2 Create ScanViewModel
  - Manage captured pages list
  - Handle image processing state
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 5.3 Create ExtractionViewModel
  - Manage extracted cards list
  - Handle deck selection/creation
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [ ] 5.4 Create DeckDetailViewModel
  - Load deck with cards and scans
  - Handle deck editing
  - _Requirements: 8.3_

- [ ] 5.5 Create StudyViewModel
  - Manage current card index
  - Track learned/review status
  - Handle filtering (all/learned/needs review)
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 5.6 Create ExportViewModel
  - Manage export format selection (TSV/CSV)
  - Generate preview data
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

### 6. Presentation Layer - UI Screens

- [ ] 6.1 Implement Home Screen (S-01)
  - [ ] 6.1.1 Create HomeScreen composable
  - [ ] 6.1.2 Implement deck list with LazyColumn
  - [ ] 6.1.3 Create empty state when no decks exist
  - [ ] 6.1.4 Add FAB with camera icon for scanning
  - _Requirements: 8.2, 1.1_

- [ ] 6.2 Implement Scan Screen (S-02)
  - [ ] 6.2.1 Create ScanScreen composable
  - [ ] 6.2.2 Integrate ML Kit Document Scanner launcher
  - [ ] 6.2.3 Display thumbnail preview of captured pages
  - [ ] 6.2.4 Allow page deletion in review mode
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 6.3 Implement Extraction Preview Screen (S-03)
  - [ ] 6.3.1 Create ExtractionPreviewScreen composable
  - [ ] 6.3.2 Display list of extracted cards with term/definition
  - [ ] 6.3.3 Enable editing mode on card tap
  - [ ] 6.3.4 Add delete and add card functionality
  - [ ] 6.3.5 Implement deck selector/prompter
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [ ] 6.4 Implement Deck Detail Screen (S-04)
  - [ ] 6.4.1 Create DeckDetailScreen composable
  - [ ] 6.4.2 Display deck title (editable), card count
  - [ ] 6.4.3 Add action buttons: Start Study, Export
  - [ ] 6.4.4 Display all cards with learned status
  - _Requirements: 8.3, 9.1, 11.1_

- [ ] 6.5 Implement Study Screen (S-05)
  - [ ] 6.5.1 Create StudyScreen composable
  - [ ] 6.5.2 Implement 3D flip card animation using graphicsLayer
  - [ ] 6.5.3 Add previous/next navigation buttons
  - [ ] 6.5.4 Display progress indicator (card X of Y)
  - [ ] 6.5.5 Add Mark as Learned / Needs Review buttons
  - [ ] 6.5.6 Implement filter for Learned/Needs Review
  - _Requirements: 9.2, 9.3, 9.4, 9.5, 10.1, 10.2, 10.3, 10.4_

- [ ] 6.6 Implement Export Screen (S-06)
  - [ ] 6.6.1 Create ExportScreen composable
  - [ ] 6.6.2 Add format selector (TSV/CSV toggle)
  - [ ] 6.6.3 Display preview of exported data
  - [ ] 6.6.4 Implement Copy to Clipboard button
  - [ ] 6.6.5 Implement Share button with Android Intent
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

### 7. Navigation

- [ ] 7.1 Set up Jetpack Navigation Compose
  - [ ] 7.1.1 Define Screen sealed class with routes
  - [ ] 7.1.2 Create NavHost with all screen destinations
  - [ ] 7.1.3 Implement navigation graph flow
  - _Requirements: All (navigation infrastructure)_

### 8. Integration - ML Kit and MediaPipe

- [ ] 8.1 Implement ML Kit Document Scanner integration
  - [ ] 8.1.1 Create DocumentScannerLauncher
  - [ ] 8.1.2 Configure DocumentScannerOptions for multi-page (max 100)
  - [ ] 8.1.3 Handle scanner results and errors
  - _Requirements: 1.1, 1.2, 1.3, 1.6, 1.7_

- [ ] 8.2 Implement ML Kit Text Recognition
  - [ ] 8.2.1 Create TextRecognizerManager
  - [ ] 8.2.2 Configure for Japanese and English recognition
  - [ ] 8.2.3 Handle recognition success/failure
  - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7_

- [ ] 8.3 Implement Image Processor
  - [ ] 8.3.1 Create ImageProcessor for corrections
  - [ ] 8.3.2 Handle perspective correction, shadow removal
  - [ ] 8.3.3 Implement manual crop fallback UI
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 8.4 Implement MediaPipe LLM Inference
  - [ ] 8.4.1 Create GemmaCardExtractor
  - [ ] 8.4.2 Implement initializeModel with LlmInference options
  - [ ] 8.4.3 Implement extractCards with prompt building
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3_

- [ ] 8.5 Implement Model Manager
  - [ ] 8.5.1 Create ModelManager for download/caching
  - [ ] 8.5.2 Implement progress indicator updates (every 2 seconds)
  - [ ] 8.5.3 Implement auto-retry up to 3 times for corruption
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

### 9. Response Parser with Fallback

- [ ] 9.1 Implement CardResponseParser
  - [ ] 9.1.1 Implement Strategy 1: Direct JSON parse
  - [ ] 9.1.2 Implement Strategy 2: Regex extraction pattern
  - [ ] 9.1.3 Implement Strategy 3: Fix common JSON issues
  - [ ] 9.1.4 Implement Strategy 4: Lenient pattern matching
  - [ ] 9.1.5 Handle escaped quotes and newlines
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9_

### 10. Export Functionality

- [ ] 10.1 Implement Quizlet Exporter
  - [ ] 10.1.1 Create exportToTsv method
  - [ ] 10.1.2 Create exportToCsv method
  - [ ] 10.1.3 Ensure Quizlet-compatible format (term first, definition second)
  - _Requirements: 11.1, 11.2, 11.5_

- [ ] 10.2 Implement PDF Generator
  - [ ] 10.2.1 Create generate method with scanned images
  - [ ] 10.2.2 Embed OCR text as searchable layer
  - [ ] 10.2.3 Add deck title as header, page numbers as footer
  - [ ] 10.2.4 Save to Documents folder
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 10.3 Implement Share Manager
  - [ ] 10.3.1 Create shareFile method with Intent
  - [ ] 10.3.2 Handle TSV, CSV, and PDF mime types

### 11. Checkpoint - Core Flow Validation
  - Ensure all tests pass, ask the user if questions arise.

### 12. Testing

- [ ] 12.1 Unit tests for CardResponseParser
  - [ ] 12.1.1 Test valid JSON parsing
  - [ ] 12.1.2 Test regex fallback with escaped quotes
  - [ ] 12.1.3 Test JSON fixing fallback
  - [ ] 12.1.4 Test lenient pattern fallback
  - [ ] 12.1.5 Test empty/invalid response handling
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9_

- [ ] 12.2 Unit tests for repositories
  - [ ] 12.2.1 Test DeckRepository CRUD operations
  - [ ] 12.2.2 Test CardRepository CRUD with deck association
  - [ ] 12.2.3 Test cascade delete behavior
  - [ ] 12.2.4 Test ScanRepository operations
  - _Requirements: 8.1, 8.3, 8.4, 15.1, 15.2, 15.3, 15.4_

- [ ] 12.3 Unit tests for use cases
  - [ ] 12.3.1 Test ScanDocumentUseCase processing pipeline
  - [ ] 12.3.2 Test ExtractCardsUseCase with mock LLM
  - [ ] 12.3.3 Test ManageDeckUseCase validation
  - [ ] 12.3.4 Test StudyCardsUseCase prioritization
  - [ ] 12.3.5 Test ExportDataUseCase format generation
  - _Requirements: All functional requirements_

- [ ] 12.4 UI tests
  - [ ] 12.4.1 Test navigation between screens
  - [ ] 12.4.2 Test flashcard flip animation
  - [ ] 12.4.3 Test deck creation and editing
  - [ ] 12.4.4 Test card editing in extraction preview

### 13. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks are organized to build incrementally: project setup → data layer → domain → presentation → integration → testing
- The 3D flip animation uses graphicsLayer with rotationY for realistic card flip effect
- All ML/AI operations run entirely on-device for privacy (Requirement 13)
- Quizlet export uses TSV/CSV format compatible with their import feature
- Property-based testing is not appropriate for this Android UI application; unit and UI tests are used instead

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1.1", "2.1.2", "2.1.3", "2.2.1", "2.2.2", "2.2.3", "2.3"] },
    { "id": 2, "tasks": ["2.4.1", "2.4.2", "2.4.3", "2.4.4", "3.1.1", "3.1.2", "3.1.3"] },
    { "id": 3, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6"] },
    { "id": 4, "tasks": ["5.1", "5.2", "5.3", "5.4", "5.5", "5.6"] },
    { "id": 5, "tasks": ["6.1.1", "6.1.2", "6.1.3", "6.1.4", "7.1.1", "7.1.2", "7.1.3"] },
    { "id": 6, "tasks": ["6.2.1", "6.2.2", "6.2.3", "6.2.4", "6.3.1", "6.3.2", "6.3.3", "6.3.4", "6.3.5"] },
    { "id": 7, "tasks": ["6.4.1", "6.4.2", "6.4.3", "6.4.4", "6.5.1", "6.5.2", "6.5.3", "6.5.4", "6.5.5", "6.5.6"] },
    { "id": 8, "tasks": ["6.6.1", "6.6.2", "6.6.3", "6.6.4", "6.6.5"] },
    { "id": 9, "tasks": ["8.1.1", "8.1.2", "8.1.3", "8.2.1", "8.2.2", "8.2.3", "8.3.1", "8.3.2", "8.3.3"] },
    { "id": 10, "tasks": ["8.4.1", "8.4.2", "8.4.3", "8.5.1", "8.5.2", "8.5.3", "9.1.1", "9.1.2", "9.1.3", "9.1.4", "9.1.5"] },
    { "id": 11, "tasks": ["10.1.1", "10.1.2", "10.1.3", "10.2.1", "10.2.2", "10.2.3", "10.2.4", "10.3.1", "10.3.2"] },
    { "id": 12, "tasks": ["11"] },
    { "id": 13, "tasks": ["12.1.1", "12.1.2", "12.1.3", "12.1.4", "12.1.5", "12.2.1", "12.2.2", "12.2.3", "12.2.4"] },
    { "id": 14, "tasks": ["12.3.1", "12.3.2", "12.3.3", "12.3.4", "12.3.5", "12.4.1", "12.4.2", "12.4.3", "12.4.4"] },
    { "id": 15, "tasks": ["13"] }
  ]
}
```
# Implementation Plan: ScanCard Android Application

## Tasks

### 1. Project Setup and Configuration
- [x] 1.1 Create Gradle wrapper and project structure
- [x] 1.2 Create app module build configuration (AGP 9.3, Kotlin 2.2)
- [x] 1.3 Create AndroidManifest.xml and initial resources
- [x] 1.4 Setup Hilt and Base Application class

### 2. Data Layer - Local Persistence
- [x] 2.1 Create Room database entities (Deck, Card, Scan)
- [x] 2.2 Create Room DAOs with Flow support
- [x] 2.3 Create AppDatabase class and Hilt providers
- [x] 2.4 Implement Repositories (Deck, Card, Scan)

### 3. Authentication & Model Management
- [x] 3.1 Integrate AppAuth-Android for Hugging Face OAuth
- [x] 3.2 Implement AuthManager for token lifecycle
- [x] 3.3 Implement ModelDownloadWorker (authenticated background download)
- [x] 3.4 Implement ModelManager with state tracking
- [x] 3.5 Create ModelConfig for Gemma 4 (E2B, E4B)

### 4. Integration - ML Kit and MediaPipe
- [x] 4.1 Implement ML Kit Document Scanner integration
- [x] 4.2 Implement ML Kit Text Recognition (Japanese/English)
- [x] 4.3 Implement GemmaCardExtractor (MediaPipe LLM Inference)
- [x] 4.4 Implement CardResponseParser with multi-strategy fallback

### 5. Domain Layer - Use Cases
- [x] 5.1 Implement ScanDocumentUseCase (OCR pipeline)
- [x] 5.2 Implement ExtractCardsUseCase (AI pipeline)
- [x] 5.3 Implement ManageDeckUseCase (CRUD)
- [x] 5.4 Implement StudyCardsUseCase (Learning logic)
- [x] 5.5 Implement ExportDataUseCase (TSV/CSV)

### 6. Presentation Layer - UI Screens
- [x] 6.1 Implement Home Screen (Deck list, manual creation)
- [x] 6.2 Implement Scan Screen (Camera integration, thumbnail grid)
- [x] 6.3 Implement Extraction Preview Screen (Model selection, OAuth, Download, AI trigger)
- [x] 6.4 Implement Deck Detail Screen (Card list, ID display for debug)
- [x] 6.5 Implement Study Screen (3D flip card animation, filters)
- [x] 6.6 Implement Export Screen (Preview, Copy, Share)

### 7. Navigation
- [x] 7.1 Setup Jetpack Navigation Compose with type-safe routes
- [x] 7.2 Implement Navigation Graph (Scan -> Extraction -> Detail flow)

### 8. Testing & Validation
- [x] 8.1 Unit tests for CardResponseParser
- [ ] 8.2 Integration tests for full Scan-to-Card flow
- [ ] 8.3 Performance profiling for 7B/E4B models on target devices
- [x] 8.4 Verification of Quizlet TSV format

---

## Completed Milestones

- Core Scan-to-Study flow is fully functional.
- Secure model distribution via Hugging Face OAuth is implemented.
- Gemma 4 mobile optimized models (E2B/E4B) are integrated.
- 3D learning experience is implemented.

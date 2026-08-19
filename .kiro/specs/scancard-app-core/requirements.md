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

## Hardware Requirements

- **Minimum RAM**: 4GB (E2B) / 8GB (E4B).
- **Storage**: Minimum 3GB - 6GB free space depending on selected model.
- **Android Version**: API 26 (Android 8.0) or higher.

---

## Requirements

### Requirement 1: Batch Document Scanning
(Identical to previous)

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

# Requirements Document

## Introduction

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document correction, and extract flashcard term/definition pairs using on-device LLMs (Gemma 4 family) - all in fully offline mode after initial setup.

## Glossary

- **ScanCard**: The Android application being developed
- **Gemma 4**: On-device LLM model family (E2B, E4B) for flashcard extraction
- **Personal Access Token (PAT)**: A Hugging Face token required to download gated models securely.
- **Deck**: A collection of flashcards organized by book title or chapter
- **Card**: A flashcard containing a term (front) and definition (back)
- **OCR**: Optical Character Recognition for text extraction from images
- **ML Kit**: Google ML Kit for document scanning and text recognition
- **LiteRT LM**: Google's modern framework for on-device LLM inference (formerly MediaPipe GenAI)
- **Quizlet**: Third-party flashcard platform supporting tab/comma-separated imports
- **OOM**: Out of Memory - application crash due to insufficient memory

## Hardware Requirements

- **Minimum RAM**: 4GB (E2B) / 8GB (E4B)
- **Storage**: Minimum 3GB - 6GB free space depending on selected model
- **Android Version**: API 26 (Android 8.0) or higher

---

## Requirements

### Requirement 1: Batch Document Scanning

**User Story:** As a user, I want to scan multiple pages of a book in one session, so that I can quickly digitize entire chapters.

#### Acceptance Criteria

1. WHEN the user taps the scan button, THE ScanCard SHALL launch the ML Kit Document Scanner with multi-page capture mode enabled.
2. WHEN the user captures pages, THE ScanCard SHALL display a thumbnail preview of each captured page.
3. WHILE the user is in review mode, THE ScanCard SHALL allow the user to review and delete individual pages.
4. WHEN the user taps the Done button, THE ScanCard SHALL process all captured pages sequentially.
5. THE ScanCard SHALL support a maximum batch size of 100 pages per scanning session.
6. THE ScanCard SHALL complete batch processing as a background task with progress indication.

---

### Requirement 2: Automatic Image Correction

**User Story:** As a user, I want scanned images to be automatically corrected, so that I get clean, readable documents without manual editing.

#### Acceptance Criteria

1. THE ScanCard SHALL rely on ML Kit Document Scanner's built-in automatic image correction features (perspective correction, shadow removal).
2. THE ML Kit Document Scanner SHALL handle document detection and perspective correction automatically.
3. WHEN image correction processing completes, THE ScanCard SHALL output corrected images.
4. THE ScanCard SHALL store images to disk cache (not in memory) to prevent OOM errors.

---

### Requirement 3: On-Device OCR Text Recognition

**User Story:** As a user, I want text to be extracted from scanned images on-device, so that my documents remain private and accessible offline.

#### Acceptance Criteria

1. WHEN a corrected image is available, THE ScanCard SHALL trigger OCR processing asynchronously.
2. THE OCR Engine SHALL recognize both Japanese and English text using ML Kit Text Recognition.
3. WHEN OCR completes successfully, THE ScanCard SHALL store the extracted raw text in the scans table.
4. THE ScanCard SHALL process images from disk cache to avoid memory issues.

---

### Requirement 4: AI Model Selection and Management

**User Story:** As a user, I want to choose the AI model that fits my device's performance, so that extraction works efficiently.

#### Acceptance Criteria

1. THE ScanCard SHALL provide a selection of AI models from the Gemma 4 family (E2B, E4B) and legacy Gemma 2.
2. THE ScanCard SHALL display the name, description, and storage size of each model.
3. IF a model is not installed, THE ScanCard SHALL prompt the user to download it from Hugging Face.
4. THE ScanCard SHALL allow the user to enter a Personal Access Token (PAT) from Hugging Face to access gated models.
5. THE ScanCard SHALL initiate model downloads using WorkManager to ensure reliability in the background, handling redirects and authentication headers properly.
6. WHILE a download is in progress, THE ScanCard SHALL display a percentage-based progress indicator.

---

### Requirement 5: Automatic Flashcard Extraction

**User Story:** As a user, I want the AI to automatically extract key terms and definitions from scanned text, so that I can quickly create study materials.

#### Acceptance Criteria

1. WHEN OCR text is available and a model is ready, THE GemmaCardExtractor SHALL send the combined text to the selected LiteRT LM model.
2. THE GemmaCardExtractor SHALL generate a JSON-formatted list of term/definition pairs.
3. THE ScanCard SHALL parse the AI response and create Card entities in the database.
4. IF the LLM returns a response that is not valid JSON, THEN the GemmaCardExtractor SHALL delegate to the Parse Fallback process (Requirement 6).

---

### Requirement 6: Parse Fallback for LLM Output

**User Story:** As a user, I want the app to handle AI output variations gracefully, so that extraction succeeds even when the AI produces non-standard JSON.

#### Acceptance Criteria

1. WHEN the initial JSON parse fails, THE Parser SHALL attempt to extract term/definition pairs using regex patterns.
2. THE Parser SHALL look for JSON arrays inside descriptive text if the AI adds introductory remarks.
3. THE Parser SHALL handle escaped characters (quotes, newlines) within term and definition fields.
4. IF all automated parsing fails, THE ScanCard SHALL display an error and allow the user to retry or create cards manually.

---

### Requirement 7: Extraction Preview and Deck Selection

**User Story:** As a user, I want to see the results of extraction and decide where to save them.

#### Acceptance Criteria

1. WHEN extraction completes, THE ScanCard SHALL display a summary of the operation.
2. THE ScanCard SHALL automatically create a new deck for the scan session (e.g., "Scan YYYY-MM-DD") if no deck was previously selected.
3. THE user SHALL be able to navigate to the deck detail screen to review all extracted cards.

---

### Requirement 8: Deck Management

**User Story:** As a user, I want to organize flashcards into decks, so that I can study different subjects separately.

#### Acceptance Criteria

1. THE ScanCard SHALL allow the user to create, edit, and delete decks.
2. THE home screen SHALL display a list of all decks sorted by last updated date.
3. WHEN a deck is deleted, THE ScanCard SHALL perform a cascade delete of all associated cards and scans.

---

### Requirement 9: Flashcard Learning with 3D Flip Animation

**User Story:** As a user, I want to study flashcards with a realistic flip animation, so that the learning experience feels engaging.

#### Acceptance Criteria

1. THE ScanCard SHALL display the card front initially and animate a 3D flip to reveal the back upon tapping.
2. THE animation SHALL use graphicsLayer for a smooth perspective effect.
3. THE user SHALL be able to navigate between cards in the deck.

---

### Requirement 10: Learning Progress Management

**User Story:** As a user, I want to track which cards I've learned.

#### Acceptance Criteria

1. THE ScanCard SHALL allow the user to mark cards as "Learned" or "Needs Review".
2. THE user SHALL be able to filter the study session by these statuses (All, Learned, Needs Review).
3. A progress indicator SHALL show the current position and total cards in the deck.

---

### Requirement 11: Export Functionality

**User Story:** As a user, I want to export flashcards to Quizlet.

#### Acceptance Criteria

1. THE ScanCard SHALL support exporting decks to TSV (Quizlet compatible) and CSV formats.
2. THE user SHALL be able to copy the exported text to the clipboard or share it via Android Intent.

---

### Requirement 12: Privacy and Offline Operation

**User Story:** As a user, I want my data to remain private and the app to work without internet.

#### Acceptance Criteria

1. THE ScanCard SHALL store all deck and card data locally in a Room database.
2. AFTER the initial model download, all core features (scanning, OCR, AI extraction, study) SHALL work fully offline.
3. THE ScanCard SHALL NOT transmit user data to external servers.
4. Edge-to-edge support SHALL be implemented for Android 15+ compatibility while ensuring system bar legibility.

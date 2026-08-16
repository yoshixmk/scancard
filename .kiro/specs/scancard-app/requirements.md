# Requirements Document

## Introduction

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document curvature correction, generate searchable PDF exports, and extract flashcard term/definition pairs using on-device LLM (Gemma Nano) - all in fully offline mode.

## Glossary

- **ScanCard**: The Android application being developed
- **Gemma Nano**: On-device LLM model for flashcard extraction
- **Deck**: A collection of flashcards organized by book title or chapter
- **Card**: A flashcard containing a term (front) and definition (back)
- **OCR**: Optical Character Recognition for text extraction from images
- **ML Kit**: Google ML Kit for document scanning and text recognition
- **MediaPipe**: Google'\''s framework for on-device ML inference
- **Quizlet**: Third-party flashcard platform supporting tab/comma-separated imports
- **OOM**: Out of Memory - application crash due to insufficient memory
- **BoundingBox**: Coordinates (x, y, width, height) defining text position in image

## Hardware Requirements

- **Minimum RAM**: 8GB (required for Gemma Nano inference)
- **Storage**: Minimum 4GB free space for model caching
- **Android Version**: API 29 (Android 10) or higher

---

## Requirements

### Requirement 1: Batch Document Scanning

**User Story:** As a user, I want to scan multiple pages of a book in one session, so that I can quickly digitize entire chapters.

#### Acceptance Criteria

1. WHEN the user taps the scan button, THE ScanCard SHALL launch the ML Kit Document Scanner with multi-page capture mode enabled supporting 2 or more pages.
2. WHEN the user captures 2 or more pages, THE ScanCard SHALL display a thumbnail preview of each captured page within 500 milliseconds.
3. WHILE the user is in review mode, THE ScanCard SHALL allow the user to review and delete individual pages before tapping the Done button.
4. WHEN the user taps the Done button, THE ScanCard SHALL process all captured pages one page at a time sequentially.
5. IF processing fails for any page, THE ScanCard SHALL display an error message with retry and cancel options.
6. THE ScanCard SHALL support a maximum batch size of 20 pages per scanning session.
7. THE ScanCard SHALL complete batch processing as a background task without blocking the UI, with progress indication.

---

### Requirement 2: Automatic Image Correction

**User Story:** As a user, I want scanned images to be automatically corrected, so that I get clean, readable documents without manual editing.

#### Acceptance Criteria

1. THE ScanCard SHALL rely on ML Kit Document Scanner'\''s built-in automatic image correction features (perspective correction, shadow removal).
2. THE ML Kit Document Scanner SHALL handle document detection, edge detection, and perspective correction automatically.
3. THE ScanCard SHALL allow the user to manually adjust crop boundaries if automatic detection does not meet user expectations.
4. WHEN image correction processing completes (automatic or manual), THE ScanCard SHALL output corrected images in PNG format.
5. IF image correction processing fails due to unsupported format or memory limitations, THEN THE ScanCard SHALL display an error message and SHALL retain the original captured image for retry.
6. THE ScanCard SHALL store images to disk cache (not in memory) to prevent OOM errors.

---

### Requirement 3: On-Device OCR Text Recognition

**User Story:** As a user, I want text to be extracted from scanned images on-device, so that my documents remain private and accessible offline.

#### Acceptance Criteria

1. WHEN a corrected image is available, THE ScanCard SHALL trigger OCR processing asynchronously without blocking the UI.
2. THE OCR Engine SHALL recognize both Japanese and English text.
3. THE OCR Engine SHALL complete text extraction within 3 seconds per page.
4. WHEN OCR completes successfully, THE ScanCard SHALL store the extracted raw text along with text bounding box coordinates in the scans table.
5. IF OCR detects fewer than 10 characters of text, THEN THE ScanCard SHALL display a warning message and allow manual text entry.
6. IF image storage fails during OCR processing, THEN THE ScanCard SHALL display an error message and allow retry.
7. IF OCR processing fails internally, THEN THE ScanCard SHALL display an error message with the option to retry or enter text manually.
8. THE ScanCard SHALL process images from disk cache to avoid memory issues.

---

### Requirement 4: Gemma Nano Model Initialization and Management

**User Story:** As a user, I want the AI model to be ready when needed, so that flashcard extraction works seamlessly.

#### Acceptance Criteria

1. WHEN the ScanCard detects no valid cached Gemma Nano model in local storage, THE ScanCard SHALL initiate download of the model files in the background with a timeout of 600 seconds.
2. WHILE the model download is in progress, THE ScanCard SHALL display a progress indicator that updates at least every 5 seconds showing download percentage or bytes received.
3. THE ScanCard SHALL support download resume (resumable downloads) if the download is interrupted.
4. WHEN the application requests flashcard extraction, THE ScanCard SHALL initialize the MediaPipe LLM Inference with the cached model within 60 seconds.
5. IF the model file is corrupted or missing, THE ScanCard SHALL attempt to re-download the model automatically up to 3 times before reporting failure.
6. WHILE a valid cached model exists, THE ScanCard SHALL use the cached model to avoid re-downloading on subsequent sessions.
7. THE ScanCard SHALL verify device meets minimum RAM requirements (8GB) before attempting model initialization.

---

### Requirement 5: Automatic Flashcard Extraction

**User Story:** As a user, I want the AI to automatically extract key terms and definitions from scanned text, so that I can quickly create study materials.

#### Acceptance Criteria

1. WHEN OCR text is available, THE GemmaCardExtractor SHALL send the text to Gemma Nano with the predefined prompt.
2. THE GemmaCardExtractor SHALL generate a JSON array containing term/definition pairs, where each pair is an object with exactly two fields: "term" (string, non-empty) and "definition" (string, non-empty).
3. THE GemmaCardExtractor SHALL complete inference within 5 seconds per page of OCR text.
4. THE ScanCard SHALL parse the JSON response and create Card entities in the database.
5. IF fewer than 1 valid term/definition pair is found, THE ScanCard SHALL display a message indicating insufficient content for extraction.
6. IF Gemma Nano returns a response that is not valid JSON, THEN the GemmaCardExtractor SHALL delegate to the Parse Fallback process (Requirement 6) to attempt extraction.
7. IF Gemma Nano fails to respond within 15 seconds, THEN the GemmaCardExtractor SHALL log an error indicating timeout and SHALL return an empty array.

---

### Requirement 6: Parse Fallback for LLM Output

**User Story:** As a user, I want the app to handle AI output variations gracefully, so that extraction succeeds even when the AI produces non-standard JSON.

#### Acceptance Criteria

1. WHEN the initial JSON parse fails, THE Parser SHALL attempt to extract term/definition pairs using the regex pattern: `\{\s*\"term\"\s*:\s*\"(.*?)\"\s*,\s*\"definition\"\s*:\s*\"(.*?)\"\s*\}`.
2. IF the regex extraction succeeds and returns at least one term/definition pair, THEN THE Parser SHALL return those pairs and stop attempting further fallback strategies.
3. IF the regex extraction fails or returns zero pairs, THE Parser SHALL attempt a second fallback strategy to fix common JSON formatting issues.
4. IF the second fallback succeeds, THE Parser SHALL attempt to parse the corrected JSON and return any valid term/definition pairs found.
5. IF the second fallback fails, THE Parser SHALL attempt a third fallback strategy to extract key-value-like patterns from the raw output using a lenient pattern matcher.
6. THE Parser SHALL stop attempting additional fallback strategies once it successfully extracts at least one valid term/definition pair.
7. THE Parser SHALL report failure only after all three fallback strategies have been exhausted without extracting any term/definition pairs.
8. IF the Parser reports failure after all fallback strategies, THE ScanCard SHALL display the raw AI output to the user and enable manual card creation.
9. WHEN attempting regex-based extraction, THE Parse Fallback SHALL handle escaped quotes (`\"`) and newline characters (`\n`) within term and definition fields.

---

### Requirement 7: Extraction Result Preview and Editing

**User Story:** As a user, I want to review and edit extracted flashcards before saving, so that I can correct any AI errors.

#### Acceptance Criteria

1. WHEN extraction completes, THE ScanCard SHALL display a list of extracted cards with term and definition fields within 5 seconds.
2. WHEN the user taps a card in the list, THE ScanCard SHALL enable editing mode for that card.
3. WHEN the user modifies a term or definition field and confirms the change, THE ScanCard SHALL update the in-memory card data.
4. WHEN the user taps the delete icon on a card, THE ScanCard SHALL remove that card from the list.
5. WHEN the user taps the Add button, THE ScanCard SHALL add a new empty card to the list.
6. WHEN the user initiates a scan, THE ScanCard SHALL prompt the user to select or create a deck first (to ensure valid deck_id for scans table).
7. IF database save operation fails, THEN THE ScanCard SHALL display an error message and retain the cards in the preview screen.
8. WHEN the user taps Save, THE ScanCard SHALL persist all cards to the database and associate them with the selected deck.
9. WHEN the save operation completes successfully, THE ScanCard SHALL display a success message to the user.

---

### Requirement 8: Deck Management

**User Story:** As a user, I want to organize flashcards into decks by book title or chapter, so that I can study different subjects separately.

#### Acceptance Criteria

1. THE ScanCard SHALL allow the user to create a new deck with a title of 1 to 100 characters.
2. THE ScanCard SHALL display a list of all decks on the home screen showing deck title, card count, and last updated date, OR display an empty state message when no decks exist.
3. THE ScanCard SHALL allow the user to edit the deck title, with title length constrained to 1 to 100 characters.
4. WHEN the user requests to delete a deck, THE ScanCard SHALL display a confirmation dialog; IF the user confirms, THE ScanCard SHALL delete the deck and all associated cards and scan history.
5. THE ScanCard SHALL sort decks in descending order by last updated date, with the most recently updated deck appearing first.

---

### Requirement 9: Flashcard Learning with 3D Flip Animation

**User Story:** As a user, I want to study flashcards with a realistic flip animation, so that the learning experience feels engaging.

#### Acceptance Criteria

1. WHEN the user starts a study session, THE ScanCard SHALL load all cards from the selected deck.
2. THE ScanCard SHALL display the card front (term) initially.
3. WHEN the user taps the card, THE ScanCard SHALL animate a 3D flip using graphicsLayer to reveal the back (definition).
4. THE flip animation SHALL complete within 300ms with perspective depth effect.
5. THE ScanCard SHALL provide navigation buttons to move to the next or previous card.

---

### Requirement 10: Learning Progress Management

**User Story:** As a user, I want to track which cards I'\''ve learned, so that I can focus on items needing review.

#### Acceptance Criteria

1. WHEN the user views the back of a card, THE ScanCard SHALL display "Mark as Learned" and "Needs Review" buttons.
2. THE ScanCard SHALL update the is_learned flag in the cards table based on user selection.
3. THE ScanCard SHALL allow the user to filter the study session to show only "Learned" or "Needs Review" cards.
4. THE ScanCard SHALL display a progress indicator showing learned vs. total cards in the deck.
5. THE ScanCard SHALL implement basic spaced repetition by prioritizing "Needs Review" cards in study sessions.

---

### Requirement 11: Quizlet Export

**User Story:** As a user, I want to export flashcards to Quizlet, so that I can study on other platforms.

#### Acceptance Criteria

1. THE ScanCard SHALL generate a tab-separated values (TSV) file containing term and definition columns.
2. THE ScanCard SHALL also support comma-separated values (CSV) format as an alternative.
3. THE ScanCard SHALL provide a "Copy to Clipboard" button that copies the exported data.
4. THE ScanCard SHALL provide a "Share" button using Android Intent to send the exported file to other apps.
5. THE exported format SHALL be compatible with Quizlet'\''s import feature (term on first column, definition on second column).

---

### Requirement 12: PDF Generation and Export

**User Story:** As a user, I want to export scanned documents as searchable PDFs, so that I can save and share digitized books.

#### Acceptance Criteria

1. THE ScanCard SHALL generate a PDF file containing all scanned images from the selected scan session.
2. THE ScanCard SHALL embed the OCR text into the PDF as a searchable text layer using bounding box coordinates.
3. THE ScanCard SHALL save the generated PDF to local storage in the device'\''s Documents folder.
4. THE ScanCard SHALL provide a "Share" button using Android Intent to send the PDF to other apps.
5. THE PDF SHALL include the deck title as the document title and page numbers in the footer.

---

### Requirement 13: Offline Operation Guarantee

**User Story:** As a user, I want all features to work without internet, so that I can study anywhere without connectivity concerns.

#### Acceptance Criteria

1. THE ScanCard SHALL function fully offline after the initial model download.
2. THE ScanCard SHALL NOT transmit any images, text, or user data to external servers.
3. THE ScanCard SHALL cache the Gemma Nano model locally after first download.
4. IF the user attempts to export without a network connection, THE ScanCard SHALL handle the operation locally (file save or Bluetooth share) without error.

---

### Requirement 14: Privacy Protection

**User Story:** As a user, I want my scanned documents and study data to remain private, so that I feel secure using the app.

#### Acceptance Criteria

1. THE ScanCard SHALL store all data locally using Room Database.
2. THE ScanCard SHALL NOT include any analytics or tracking SDKs that transmit user data.
3. THE ScanCard SHALL allow the user to export all their data as a backup file.
4. THE ScanCard SHALL allow the user to delete all data from the app with a single action.
5. THE ScanCard SHALL request only camera permissions, with clear explanation of why camera access is needed.

---

### Requirement 15: Database Schema Persistence

**User Story:** As a developer, I want the database schema to persist all application data, so that user progress is saved between sessions.

#### Acceptance Criteria

1. THE decks table SHALL persist deck records with id, title, created_at, and updated_at fields.
2. THE cards table SHALL persist card records with id, deck_id (FK), term, definition, is_learned, created_at, and optional scan_id fields.
3. THE scans table SHALL persist scan records with id, deck_id (FK, NOT NULL), image_path, raw_text, and bounding_box_data (JSON string) fields.
4. THE database SHALL enforce referential integrity, preventing deletion of decks that have associated cards or scans.
5. THE database SHALL use migrations for schema updates to preserve user data during app updates.

---

### Requirement 16: Memory Management and Performance

**User Story:** As a user, I want the app to handle large batch scans without crashing due to memory issues.

#### Acceptance Criteria

1. THE ScanCard SHALL store captured images to disk cache (not in memory/bitmap) and reference them by file path.
2. THE ScanCard SHALL use downsampled thumbnails for display in the UI.
3. THE ScanCard SHALL process images one at a time sequentially to minimize memory footprint.
4. THE ScanCard SHALL implement proper bitmap recycling and clear references after use to prevent memory leaks.
5. IF device has less than 8GB RAM, THE ScanCard SHALL warn the user that performance may be affected.

---

### Requirement 17: Model Distribution and Download

**User Story:** As a user, I want the AI model to be downloaded reliably, even on slower connections.

#### Acceptance Criteria

1. THE ScanCard SHALL download the Gemma Nano model from a reliable CDN or bundled asset.
2. THE ScanCard SHALL support resumable downloads to handle interrupted connections.
3. THE ScanCard SHALL verify model integrity (checksum) after download.
4. THE ScanCard SHALL display download progress with estimated time remaining.
5. IF download fails repeatedly, THE ScanCard SHALL provide a manual retry option.

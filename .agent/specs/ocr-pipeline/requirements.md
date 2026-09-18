# Requirements Document — ScanCard OCR Pipeline (ML Kit + Gemma Word Generation)

## Introduction

To accelerate the ScanCard extraction pipeline, OCR and word generation are separated. OCR runs on Google ML Kit Text Recognition (offline, high-accuracy, fast, supports Japanese vertical/horizontal writing, unbundled delivery via Play Services to keep app size small) and word (term/definition) generation is delegated to Gemma (LiteRT LM). This spec separates OCR/word-generation responsibilities from the existing `app-core` and defines the background execution model. `appium/images/sample.jpg` is used for E2E verification that pre-OCR behaves as expected.

## Glossary

- **ML Kit Text Recognition**: Google ML Kit text recognition. Unbundled (`play-services-mlkit-text-recognition` + language-specific model) — dynamically fetched via Play Services, not bundled in the APK
- **Japanese Text Recognition**: `com.google.mlkit:text-recognition-japanese` (supports vertical/horizontal writing)
- **OCR**: Process of extracting strings from images
- **Word Generation**: Process of generating word cards (`term`/`definition`/`japaneseTranslation`) from OCR text (Gemma/LiteRT LM)
- **Pipeline**: The sequence OCR → Word Generation → Persistence
- **Sample Image**: `appium/images/sample.jpg` (for ML Kit pre-OCR verification)
- **Foreground Pipeline**: Pipeline that completes in the UI foreground without WorkManager
- **Virtual Scene**: Emulator virtual scene (`hw.camera.back=virtualscene`) where `images/sample.jpg` is injected via `adb emu virtualscene-image`

## Requirements

### Requirement 1: ML Kit OCR (Offline, Unbundled)

**User Story:** As a user, I want OCR to run offline quickly and accurately regardless of document orientation, without increasing app size.

#### Acceptance Criteria

1. THE ScanCard SHALL perform OCR via Google ML Kit Text Recognition using the **unbundled** delivery (`com.google.android.gms:play-services-mlkit-text-recognition` + `com.google.mlkit:text-recognition-japanese`), NOT the bundled `com.google.mlkit:text-recognition` that embeds the model in the APK. The language-specific model SHALL be fetched dynamically via Google Play Services.
2. THE ML Kit OCR SHALL support Japanese vertical and horizontal writing and English mixed text contained in `sample.jpg` in a single recognizer call without per-orientation branching.
3. THE `TextRecognitionManager` SHALL expose `suspend fun recognizeText(imageUri: Uri): String` and `recognizeTextFromBitmap(Bitmap): String` that internally downscale the bitmap to max 1080 long edge (`downscaleIfNeeded`), then `InputImage.fromBitmap` on `Dispatchers.IO` via `TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())`, returning concatenated `textBlocks` in reading order; empty image returns `""`.
4. THE OCR SHALL be fully offline after the language model is cached by Play Services; no network call SHALL be required at recognition time.
5. WHEN the Japanese model is not yet downloaded, THEN the call SHALL await Play Services download (via `ModuleInstall` / on-demand) and retry, surfacing `MlKitModelNotReadyException` only if download fails after one retry.

### Requirement 2: Gemma Word Generation from OCR Text

**User Story:** As a user, I want the time from shooting to card list to be short because only word creation uses the LLM.

#### Acceptance Criteria

1. WHEN OCR text (Req1) is available (non-empty), THEN `GemmaCardExtractor` SHALL receive **only** the OCR text (no image) and generate `List<ExtractedCard(term, definition, japaneseTranslation)>` via LiteRT LM; image-to-text SHALL NOT be delegated to Gemma.
2. THE prompt SHALL be a text-only word-generation prompt: OCR text + fixed instruction to extract term/definition pairs with bilingual translation. Image bytes SHALL NOT be included.
3. WHEN OCR text is empty or whitespace-only, THEN Word Generation SHALL be skipped without invoking LiteRT LM: a blank deck completes as `COMPLETED` with no model init, and blank pages are skipped while still advancing progress.
4. THE word generation output SHALL be parsed by `CardResponseParser` with 3 fallback strategies (direct JSONArray, bracket-substring, regex); single prompt per page with JSON format example ensures one-shot success without retry.
5. THE end-to-end latency from `recognizeText` return to `insertCards` completion for a single page (`sample.jpg` equivalent) SHALL be dominated by word generation only; OCR time SHALL NOT include LLM initialization (model already `Ready`).

### Requirement 3: Pipeline Separation and Execution Model

**User Story:** As a developer, I want OCR and word generation to be independently testable and replaceable, and the execution model (foreground vs background) to be explicitly defined.

#### Acceptance Criteria

1. THE responsibilities SHALL be separated: `TextRecognitionManager` (OCR) and `GemmaCardExtractor` (word generation) SHALL have no circular dependency and SHALL be injectable separately via Hilt; `app-core` SHALL NOT own OCR/word-generation logic — this spec is the single source of truth.
2. THE pipeline orchestrator SHALL be `ScanDocumentUseCase.processScannedPages` (OCR, parallel, order-preserved) followed by `ExtractCardsUseCase.extractAndSaveCards` (single prompt per page, page-by-page) via `BackgroundTaskManager`/`CardExtractionWorker` (WorkManager `dataSync`); no foreground `OcrWordPipelineUseCase` is retained.
3. THE pipeline SHALL run via WorkManager `dataSync` with `extraction_channel` progress notifications (`Page n of m`, app-managed id `deckId+100_000`) as defined in `app-core` Req12.
4. WHEN OCR text is empty, THEN Word Generation SHALL be skipped without invoking LiteRT LM and the deck status SHALL still become `COMPLETED` to avoid resume loops.
5. THE E2E fast path SHALL exercise the same WorkManager pipeline via DEBUG dummy scan/model helpers.

### Requirement 4: E2E Verification with Sample Image

**User Story:** As a QA, I want an automated E2E that proves ML Kit pre-OCR works on the provided sample before word generation.

#### Acceptance Criteria

1. THE repo SHALL keep `appium/images/sample.jpg` as the canonical sample for pre-OCR verification; `appium/helpers/virtualScene.js` SHALL inject it to the emulator via `adb emu virtualscene-image wall <host-abs-path>` in `wdio.conf.js:67` `onPrepare` and reset in `onComplete` (existing mechanism).
2. AN E2E spec `appium/specs/mlkitOcrSample.e2e.js` SHALL exist and: (a) launch the app, (b) trigger OCR on `sample.jpg` via HomeScreen DEBUG helper `testMlkitOcrSampleBtn` (assets/sample.jpg → `TextRecognitionManager.recognizeTextFromBitmap` with 1080 downscale, no scanner), (c) assert OCR text is non-empty and contains `Kotlin` AND `Android` (case-insensitive) and at least one of `Programming`/`coroutines`/`O'REILLY`, (d) assert the OCR result alone is surfaced via `ocrResultText` (word generation is covered by unit tests and the WorkManager E2E).
3. THE E2E SHALL verify OCR isolation: it SHALL assert that OCR succeeds even when Gemma model state is `Idle` (no LLM), proving OCR does not depend on Gemma.
4. THE E2E invokes no Gemma and carries no `@slow` tag; it SHALL run green under both `wdio.conf.js` and `wdio.slow.conf.js`.
5. FAILURE mode: IF OCR returns empty on `sample.jpg`, THEN E2E SHALL fail with a page-source diagnostic dump and SHALL NOT proceed to word generation.

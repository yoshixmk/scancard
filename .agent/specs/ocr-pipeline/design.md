# Design Document — ScanCard OCR Pipeline (ML Kit + Gemma)

## Overview

OCR/word generation is separated from `scancard-app-core`: ML Kit (unbundled, Japanese) owns OCR, and Gemma (LiteRT LM) owns text-to-word generation only. The pipeline is executed as `ScanDocumentUseCase` (OCR, parallel) followed by `ExtractCardsUseCase`/`BackgroundTaskManager` (WorkManager `dataSync`). `appium/images/sample.jpg` is verified via Virtual Scene E2E.

## Architecture

```mermaid
graph TD
    UI[ScanScreen / ScanViewModel] --> SCAN[ScanDocumentUseCase - OCR parallel]
    SCAN --> OCR[TextRecognitionManager - ML Kit Japanese]
    UI --> BTM[BackgroundTaskManager - WorkManager dataSync]
    BTM --> WORD[GemmaCardExtractor - LiteRT LM]
    BTM --> REPO[CardRepository / Deck.extractionStatus]
    OCR --> MS[Play Services - TextRecognition Japanese Model]
    WORD --> LM[LiteRT LM Engine - Gemma 4 E2B]
    BTM --> N[NotificationHelper - extraction_channel]
    SAMPLE[appium/images/sample.jpg] --> VS[virtualScene.js - adb emu virtualscene-image]
    VS --> EMU[Emulator wall]
    EMU --> OCR
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| OCR | `play-services-mlkit-text-recognition:19.0.1` + `text-recognition-japanese:16.0.1` | Unbundled Japanese OCR (vertical/horizontal) |
| Word Gen | `litertlm-android:0.16.1` + `ai-delivery` | Gemma text→words only |
| Orchestration | `ScanDocumentUseCase` + `ExtractCardsUseCase`/`BackgroundTaskManager` (Hilt) | OCR (parallel, order-preserved) → Word→Persistence via WorkManager |
| Background | WorkManager `dataSync` | Same as scancard-app-core Req12, reuse NotificationHelper |
| E2E | `virtualScene.js` + `mlkitOcrSample.e2e.js` | Sample image verification |

## Components and Interfaces

### ITextRecognitionManager

```kotlin
interface ITextRecognitionManager {
    suspend fun recognizeText(imageUri: Uri): String
    // Vertical/horizontal Japanese + English in one call, unbundled
}
```

- Impl `TextRecognitionManager`: singleton client built via `JapaneseTextRecognizerOptions.Builder().build()`, executed on `withContext(Dispatchers.IO)` after `downscaleIfNeeded(1080)` then `InputImage.fromBitmap` → `client.process(image).await()`. Uri is downscaled at decode time via `loadBitmapScaled(1080)`; Bitmap is downscaled directly. If the model is not yet cached, `ModuleInstall` `requestInstall` is tried once.

### IWordGenerationExtractor

```kotlin
interface IWordGenerationExtractor {
    suspend fun isReady(): Boolean
    suspend fun generateWords(ocrText: String): List<ExtractedCard>
}
```

- Impl wraps `GemmaCardExtractor`. The prompt is `ocrText` only (no image bytes). Empty text returns immediately with an empty list. Invalid entries are filtered by `PromptValidator` with one retry.

### Pipeline Orchestration

- `ScanDocumentUseCase.processScannedPages(deckId, uris)` runs OCR for all pages in parallel via `coroutineScope { uris.map { async { ocr.recognizeText } }.awaitAll() }`, preserving page order, then inserts `Scan` rows.
- `ExtractCardsUseCase.extractAndSaveCards(deckId, modelConfig, onProgress)` (invoked via `BackgroundTaskManager.startExtraction` / `CardExtractionWorker`) processes scans page-by-page for word generation, validates with `PromptValidator`, dedupes via `CardValidator`, inserts `Card` rows, and updates `Deck.extractionStatus` to `COMPLETED`. Progress is posted via WorkManager `setProgress` and `NotificationHelper` (`deckId+100_000`).

## Data Models

```kotlin
data class ExtractedCard(val term: String, val definition: String, val japaneseTranslation: String, val confidence: Float = 1f)
```

`Deck.extractionStatus` reuses the existing `NONE→RUNNING→COMPLETED/FAILED` (Room). OCR success with zero words is also `COMPLETED` (empty deck is allowed).

## Correctness Properties

- OCR returns non-empty text for `sample.jpg` (including vertical writing).
- WordGen generates from OCR text only, without referencing the image.
- If OCR is empty, LLM is not called.

## Error Handling

- `MlKitModelNotReady`: retry `ModuleInstall.requestInstall` once, then surface `MlKitModelNotReady` only if download fails after one retry.
- `InputImage` failure: log and treat as `""` for the next page.
- LLM failure: filtered by `PromptValidator`, one retry, skip if still invalid.
- WorkManager: `CancellationException` is rethrown for rescheduling.

## Testing Strategy

- Unit: verify `TextRecognitionManager.recognizeText` delegates to `Dispatchers.IO` and handles empty inputs via `Robolectric` + `MockK`.
- E2E: `mlkitOcrSample.e2e.js` injects `sample.jpg` via Virtual Scene → asserts non-empty `recognizeText` + (if model Ready) ≥1 card. Asserts `Idle` still passes OCR non-empty to prove isolation. `@slow` only when Gemma is invoked.
- Manual: verify initial Play Services model download wait time.

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| play-services-mlkit-text-recognition | 19.0.1 | Unbundled base |
| text-recognition-japanese | 16.0.1 | Japanese vertical/horizontal |
| kotlinx-coroutines-play-services | 1.11.0 | `await()` |
| play-services-base / module-install | latest | Model on-demand |
| litertlm-android | 0.16.1 | Gemma word gen |
| ai-delivery | 0.2.0-beta01 | Gemma model |

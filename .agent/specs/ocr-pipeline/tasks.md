# Tasks — ScanCard OCR Pipeline (ML Kit + Gemma)

## Dependency Graph

```json
{
  "waves": [
    { "id": "1", "tasks": ["1.1", "1.2"] },
    { "id": "2", "tasks": ["2.1", "2.2"] },
    { "id": "3", "tasks": ["3.1", "3.2"] }
  ]
}
```

## Tasks

### 1. ML Kit OCR Foundation (Requirement 1)

- [x] 1.1 Switch `TextRecognitionManager` to Japanese unbundled (`JapaneseTextRecognizerOptions` singleton, `Dispatchers.IO` `downscaleIfNeeded(1080)`→`InputImage.fromBitmap`, `ModuleInstall` retry, `MlKitModelNotReadyException` handling) and cover orchestration with `ScanDocumentUseCaseTest` (parallel, order-preserved, mocked OCR)
- [x] 1.2 Verify unbundled dependencies (`play-services-mlkit-text-recognition` + `text-recognition-japanese` + `play-services-base` only, do not use bundled `text-recognition`) and keep `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` green

### 2. Gemma Word Generation Separation (Requirements 2, 3)

- [x] 2.1 Separate word generation to text-only prompts (skip empty deck/pages without LLM, single prompt per page with JSON example, `CardResponseParser` 3 fallbacks, no image bytes, `ExtractCardsUseCaseTest`) and verify separation (`TextRecognitionManager` and `GemmaCardExtractor` independently injectable via Hilt, no circular dependency)
- [x] 2.2 Verify pipeline separation via `ScanDocumentUseCase` (OCR, parallel, order-preserved) + `ExtractCardsUseCase`/`BackgroundTaskManager` (WorkManager `dataSync`); no foreground `StateFlow` pipeline retained

### 3. E2E Verification with Sample Image (Requirement 4)

- [x] 3.1 Create `mlkitOcrSample.e2e.js` for `appium/images/sample.jpg` (HomeScreen DEBUG helper `testMlkitOcrSampleBtn`→`ocrResultText`, non-empty/containment assertions `Kotlin`+`Android`+`Programming|coroutines|O'REILLY`, verify OCR isolation even when Gemma is Idle)
- [x] 3.2 Make E2E green (`npx wdio run wdio.conf.js --spec specs/mlkitOcrSample.e2e.js` 2 passing 6.8s/6.7s, Virtual Scene maintained with Toren1BD.posters custom 0 0 -1.5) and complete improvement loop

## Implementation Order

1 → 2 → 3

## Notes

- This spec is the single source of truth for OCR/word generation. Descriptions in `app-core` are delegated to this spec (this spec takes priority on conflicts; core keeps only references after migration).
- Pipeline is executed via `ScanDocumentUseCase` + `ExtractCardsUseCase`/`BackgroundTaskManager` (WorkManager `dataSync`, notification ID `deckId+100_000` same as core); no foreground pipeline retained.
- Virtual Scene auto-injection is hands-free via `helpers/virtualScene.js:34` `adb emu virtualscene-image`.

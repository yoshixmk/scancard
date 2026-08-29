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

- [x] 1.1 `TextRecognitionManager` を Japanese unbundled に切替（`JapaneseTextRecognizerOptions` singleton, `Dispatchers.IO` `downscaleIfNeeded(1080)`→`InputImage.fromBitmap`, `ModuleInstall` retry, `MlKitModelNotReady` ハンドリング）と単体テスト（empty/正常/縦書きブロック順）
- [x] 1.2 依存の unbundled 化確認（`play-services-mlkit-text-recognition` + `text-recognition-japanese` + `play-services-base` のみ、bundled `text-recognition` を使わない）と `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` 緑化

### 2. Gemma Word Generation Separation (Requirements 2, 3)

- [x] 2.1 `IWordGenerationExtractor`/`GemmaCardExtractor` をテキストのみプロンプトに分離（空文字 skip, `PromptValidator` retry 1回, 画像バイト非参照）と `OcrWordPipelineUseCase`（`execute` foreground `StateFlow` + `enqueueBackground` opt-in WorkManager, page順序保持）
- [x] 2.2 `ScanViewModel` に `OcrWordPipelineUseCase` を注入し `ocrPipelineState` と `processScansWithPipeline` を追加、`scancard-app-core` からの責務分離（本specが単一真実源）

### 3. E2E Verification with Sample Image (Requirement 4)

- [x] 3.1 `appium/images/sample.jpg` を前提とした `mlkitOcrSample.e2e.js` 作成（HomeScreen DEBUG helper `testMlkitOcrSampleBtn`→`ocrResultText`、非空/包含アサート `Kotlin`+`Android`+`Programming|coroutines|O'REILLY`、Gemma IdleでもOCR成功を分離検証）
- [x] 3.2 E2E 緑化（`npx wdio run wdio.conf.js --spec specs/mlkitOcrSample.e2e.js` 2 passing 6.8s/6.7s, Toren1BD.posters custom 0 0 -1.5 でVirtual Sceneも整備）と改善ループ完了

## Implementation Order

1 → 2 → 3

## Notes

- 本specが OCR/単語生成の単一真実源。`scancard-app-core` の該当記述は本specに委譲されたものとして扱う（重複時は本spec優先、移行後に core 側は参照に留める）。
- バックグラウンド実行は既定 foreground、opt-in のみ WorkManager（`dataSync`, `deckId+100_000` 通知IDは core と同一）。
- Virtual Scene 自動投入は `helpers/virtualScene.js:34` `adb emu virtualscene-image` で手動 import 不要。

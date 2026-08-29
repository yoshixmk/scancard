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

- [ ] 1.1 `TextRecognitionManager` を Japanese unbundled に切替（`JapaneseTextRecognizerOptions` singleton, `Dispatchers.IO` `InputImage.fromFilePath`, `ModuleInstall` retry, `MlKitModelNotReady` ハンドリング）と単体テスト（empty/正常/縦書きブロック順）
- [ ] 1.2 依存の unbundled 化確認（`play-services-mlkit-text-recognition` + `text-recognition-japanese` のみ、bundled `text-recognition` を使わない）と `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` 緑化

### 2. Gemma Word Generation Separation (Requirements 2, 3)

- [ ] 2.1 `IWordGenerationExtractor`/`GemmaCardExtractor` をテキストのみプロンプトに分離（空文字 skip, `PromptValidator` retry 1回, 画像バイト非参照）と `OcrWordPipelineUseCase`（`execute` foreground `StateFlow` + `enqueueBackground` opt-in WorkManager, page順序保持）
- [ ] 2.2 `ScanViewModel` から `OcrWordPipelineUseCase.execute` を呼ぶ配線と `scancard-app-core` からの責務分離（重複ロジック削除、Hilt bind 更新）

### 3. E2E Verification with Sample Image (Requirement 4)

- [ ] 3.1 `appium/images/sample.jpg` を前提とした `mlkitOcrSample.e2e.js` 作成（Virtual Scene 投入済みの `wdio.conf.js:67` を利用、DEBUG helper `testMlkitOcrSampleBtn` または gallery 経由で OCR 実行、非空/包含アサート、Gemma Idle でも OCR 成功を分離検証、`@slow` 分岐）
- [ ] 3.2 E2E 緑化（`mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/mlkitOcrSample.e2e.js` と `wdio.slow.conf.js` の両方、失敗時の `blocks/lines` ダンプ出力確認）と改善ループ（E2E完了まで反復）

## Implementation Order

1 → 2 → 3

## Notes

- 本specが OCR/単語生成の単一真実源。`scancard-app-core` の該当記述は本specに委譲されたものとして扱う（重複時は本spec優先、移行後に core 側は参照に留める）。
- バックグラウンド実行は既定 foreground、opt-in のみ WorkManager（`dataSync`, `deckId+100_000` 通知IDは core と同一）。
- Virtual Scene 自動投入は `helpers/virtualScene.js:34` `adb emu virtualscene-image` で手動 import 不要。

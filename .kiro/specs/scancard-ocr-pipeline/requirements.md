# Requirements Document — ScanCard OCR Pipeline (ML Kit + Gemma Word Generation)

## Introduction

ScanCardの抽出パイプラインを高速化するため、OCRと単語生成を分離する。OCRは Google ML Kit Text Recognition（オフライン・高精度・高速・日本語縦書き/横書き対応・Play Services経由の unbundled 配信でアプリサイズ小）で実行し、単語（term/definition）生成のみを Gemma (LiteRT LM) に委譲する。既存の `scancard-app-core` から OCR/単語生成の責務を本specへ分離し、バックグラウンド実行の扱いも本specで定義する。`appium/images/sample.jpg` を用いたE2Eで、事前OCRが想定通り動作することを検証する。

## Glossary

- **ML Kit Text Recognition**: Google ML Kit のテキスト認識。unbundled（`play-services-mlkit-text-recognition` + 言語別モデル）でPlay Services経由に動的取得、アプリバイナリに同梱しない
- **Japanese Text Recognition**: `com.google.mlkit:text-recognition-japanese`（縦書き/横書き対応）
- **OCR**: 画像から文字列を抽出する処理
- **Word Generation**: OCRテキストから `term`/`definition`/`japaneseTranslation` の単語カードを生成する処理（Gemma/LiteRT LM）
- **Pipeline**: OCR → Word Generation → Persistence の一連の流れ
- **Sample Image**: `appium/images/sample.jpg`（ML Kit事前OCR検証用）
- **Foreground Pipeline**: UIフォアグラウンドで完結するパイプライン（WorkManagerを使わない）
- **Virtual Scene**: エミュレータの仮想シーン（`hw.camera.back=virtualscene`）に `images/sample.jpg` を `adb emu virtualscene-image` で投入

## Requirements

### Requirement 1: ML Kit OCR (Offline, Unbundled)

**User Story:** As a user, I want OCR to run offline quickly and accurately regardless of document orientation, without increasing app size.

#### Acceptance Criteria

1. THE ScanCard SHALL perform OCR via Google ML Kit Text Recognition using the **unbundled** delivery (`com.google.android.gms:play-services-mlkit-text-recognition` + `com.google.mlkit:text-recognition-japanese`), NOT the bundled `com.google.mlkit:text-recognition` that embeds the model in the APK. The language-specific model SHALL be fetched dynamically via Google Play Services.
2. THE ML Kit OCR SHALL support Japanese vertical and horizontal writing and English mixed text contained in `sample.jpg` in a single recognizer call without per-orientation branching.
3. THE `TextRecognitionManager` SHALL expose `suspend fun recognizeText(imageUri: Uri): String` that internally uses `InputImage.fromFilePath` on `Dispatchers.IO` and `TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())`, returning concatenated `textBlocks` in reading order; empty image returns `""`.
4. THE OCR SHALL be fully offline after the language model is cached by Play Services; no network call SHALL be required at recognition time.
5. WHEN the Japanese model is not yet downloaded, THEN the call SHALL await Play Services download (via `ModuleInstall` / on-demand) and retry, surfacing `MlKitModelNotReady` only if download fails after one retry.

### Requirement 2: Gemma Word Generation from OCR Text

**User Story:** As a user, I want the time from shooting to card list to be short because only word creation uses the LLM.

#### Acceptance Criteria

1. WHEN OCR text (Req1) is available (non-empty), THEN `GemmaCardExtractor` SHALL receive **only** the OCR text (no image) and generate `List<ExtractedCard(term, definition, japaneseTranslation)>` via LiteRT LM; image-to-text SHALL NOT be delegated to Gemma.
2. THE prompt SHALL be a text-only word-generation prompt: OCR text + fixed instruction to extract term/definition pairs with bilingual translation. Image bytes SHALL NOT be included.
3. WHEN OCR text is empty or whitespace-only, THEN Word Generation SHALL be skipped and return empty list without invoking LiteRT LM.
4. THE word generation output SHALL be validated by `PromptValidator` (term appears verbatim, no generic "A topic to Explore"); invalid entries SHALL be dropped or retried once with an improved prompt, then persisted only if valid.
5. THE end-to-end latency from `recognizeText` return to `insertCards` completion for a single page (`sample.jpg` equivalent) SHALL be dominated by word generation only; OCR time SHALL NOT include LLM initialization (model already `Ready`).

### Requirement 3: Pipeline Separation and Execution Model

**User Story:** As a developer, I want OCR and word generation to be independently testable and replaceable, and the execution model (foreground vs background) to be explicitly defined.

#### Acceptance Criteria

1. THE responsibilities SHALL be separated: `TextRecognitionManager` (OCR) and `GemmaCardExtractor`/`WordGenerationUseCase` (word generation) SHALL have no circular dependency and SHALL be injectable separately via Hilt; `scancard-app-core` SHALL NOT own OCR/word-generation logic — this spec is the single source of truth.
2. THE pipeline orchestrator `OcrWordPipelineUseCase` SHALL compose `recognizeText` → `generateWords` → `cardRepository.insertCards` sequentially per page, preserving page order.
3. THE pipeline SHALL run in **foreground** by default (no WorkManager) for the fast path: invoked from `ScanViewModel` on the UI scope and cancelled when the scope is cancelled. A background variant (WorkManager `dataSync`) MAY be provided as an opt-in overload `OcrWordPipelineUseCase.enqueueBackground(deckId, imageUris)` that reuses the same OCR→word steps and posts the same `extraction_channel` progress notifications as `scancard-app-core` Req12, but the E2E fast path SHALL NOT require WorkManager.
4. WHEN pipeline is foreground, THEN progress SHALL be reported via `StateFlow<OcrPipelineState>` (Idle / Ocring(page) / Generating(page) / Completed(cards)) for Compose observation, not via system notification.
5. WHEN pipeline is background (opt-in), THEN it SHALL post the same progress notifications (`Page n of m`, app-managed id `deckId+100_000`) as existing `BackgroundTaskManager`, and SHALL survive process death via WorkManager retry.

### Requirement 4: E2E Verification with Sample Image

**User Story:** As a QA, I want an automated E2E that proves ML Kit pre-OCR works on the provided sample before word generation.

#### Acceptance Criteria

1. THE repo SHALL keep `appium/images/sample.jpg` as the canonical sample for pre-OCR verification; `appium/helpers/virtualScene.js` SHALL inject it to the emulator via `adb emu virtualscene-image wall <host-abs-path>` in `wdio.conf.js:67` `onPrepare` and reset in `onComplete` (existing mechanism).
2. AN E2E spec `appium/specs/mlkitOcrSample.e2e.js` SHALL exist and: (a) launch the app, (b) trigger OCR on `sample.jpg` without manual Android Studio import — either via `ScanScreen` gallery import / virtual-scene capture or via a DEBUG helper `testMlkitOcrSampleBtn` that directly calls `TextRecognitionManager.recognizeText` on the bundled asset, (c) assert OCR text is non-empty and contains at least one word from `sample.jpg` — canonical `sample.jpg` is the O'Reilly "Programming Android with Kotlin" back cover, so assertion SHALL check for `Kotlin` AND `Android` (case-insensitive), and at least one of `Programming`/`coroutines`/`O'REILLY`, (d) assert word generation (if model Ready) produces ≥1 card, otherwise assert OCR result alone persists and `extractionStatus` remains `COMPLETED` for the OCR step.
3. THE E2E SHALL verify OCR isolation: it SHALL assert that OCR succeeds even when Gemma model state is `Idle` (no LLM), proving OCR does not depend on Gemma.
4. THE E2E SHALL tag `mlkitOcrSample` as `@slow` if it invokes Gemma (multi-minute), otherwise as normal; both paths SHALL be runnable via `mise exec node@20 -- npx wdio run wdio.slow.conf.js --spec specs/mlkitOcrSample.e2e.js` and `wdio.conf.js`.
5. FAILURE mode: IF OCR returns empty on `sample.jpg`, THEN E2E SHALL fail with diagnostic dump of `TextRecognition` result (`blocks`, `lines`) and SHALL NOT proceed to word generation.


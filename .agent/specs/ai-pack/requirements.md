# Requirements Document — ScanCard AI Pack Delivery (Gemma 4 E2B)

## Introduction

ScanCard delivers the on-device LLM (Gemma 4 E2B, 2.6GB) via Google Play AI Delivery as two on-demand AI Packs. `ModelManager` owns the pack lifecycle (status query, download, progress aggregation, part assembly), `ModelRepository` exposes it as `StateFlow<ModelState>`, and `ExtractionPreviewScreen` renders Idle / Downloading(%) / Ready / Error. This spec is the single source of truth for AI-pack behavior; `app-core` Requirement 4 is an overview only.

## Glossary

- **AI Pack**: A downloadable bundle containing AI model assets, delivered via Google Play.
- **AiPackManager / AiPackState / AiPackStatus**: Play `ai-delivery` APIs for download state (`PENDING`, `DOWNLOADING`, `TRANSFERRING`, `COMPLETED`, `FAILED`, `CANCELED`, `WAITING_FOR_WIFI`, `REQUIRES_USER_CONFIRMATION`, `NOT_INSTALLED`, `UNKNOWN`).
- **ModelConfig**: Pack set definition (`aiPackNames`, `partFileNames`, `fileName`, `sizeGb`).
- **ModelState**: UI-facing state (`Idle`, `Downloading(progress 0f..1f)`, `Ready`, `Error(message)`).
- **Assembled file**: `filesDir/gemma-4-E2B-it.litertlm` produced by concatenating the parts.
- **Part file**: `gemma-4-E2B-it.litertlm.part0` in `gemma_ai_pack`, `gemma-4-E2B-it.litertlm.part1` in `gemma_ai_pack_2`.
- **PACK_UNAVAILABLE(-2)**: Play error when the pack is not contained in the delivered `.aab`.

## Constraints

- Play compressed limit is 1.5GB per pack; the 2.6GB model is always split into 2 packs.
- Minimum storage is 3GB free space for the E2B model.
- Minimum Android API is 26.

---

## Requirements

### Requirement 1: Pack Packaging and On-Demand Delivery

**User Story:** As a user, I want the model delivered securely via Google Play without external authentication.

#### Acceptance Criteria

1. THE ScanCard SHALL provide exactly two `com.android.ai-pack` modules with `deliveryType = "on-demand"`: pack `gemma_ai_pack` holding `gemma-4-E2B-it.litertlm.part0` and pack `gemma_ai_pack_2` holding `gemma-4-E2B-it.litertlm.part1`.
2. THE app module SHALL declare `assetPacks.add(":gemma-ai-pack")` and `assetPacks.add(":gemma-ai-pack-2")` so the `.aab` contains both packs; a missing pack SHALL surface as `PACK_UNAVAILABLE(-2)`.
3. THE ScanCard SHALL follow the single-model policy: `ModelConfig.GEMMA_4_E2B` (`id = "gemma-4-e2b"`, `sizeGb = 2.6`) is the only entry of `AVAILABLE_MODELS`.
4. THE ScanCard SHALL use `com.google.android.play:ai-delivery:0.2.0-beta01` for all pack operations.
5. THE ScanCard SHALL NOT require external authentication (such as a Hugging Face PAT); delivery relies on system-level Play distribution.

### Requirement 2: Status Query and Ready Detection

**User Story:** As a user, I want the app to know whether the model is ready without manual file management.

#### Acceptance Criteria

1. WHEN `filesDir/gemma-4-E2B-it.litertlm` exists (release assembly or DEBUG dummy), THEN `checkModelStatus` SHALL report `Ready` without calling Play.
2. WHEN no assembled file exists, THEN `checkModelStatus` SHALL set the active config, ensure the single progress listener is registered, and query `getPackStates(aiPackNames)`.
3. WHEN all packs report `COMPLETED`, THEN THE ModelManager SHALL attempt `tryAssembleSplitModel` and report `Ready`.
4. ALL Play `Task` callbacks SHALL run on a direct executor (calling thread), with no `MainLooper` dependency, so unit tests observe state synchronously.

### Requirement 3: Download Trigger and Progress Aggregation

**User Story:** As a user, I want the download button to start delivery and the percentage to keep increasing until completion.

#### Acceptance Criteria

1. WHEN `downloadModel` is called, THEN THE ModelManager SHALL set the active config, ensure the listener is registered, call `fetch(aiPackNames)`, reflect the fetch-result snapshot via `handlePackStates`, and re-query `getPackStates`; subsequent progress SHALL be driven by listener callbacks.
2. THE ModelManager SHALL register exactly one `AiPackStateUpdateListener`; on each callback whose `state.name()` belongs to the active config, it SHALL re-query `getPackStates` and aggregate across both packs. A one-shot query alone SHALL NOT be the progress mechanism.
3. THE aggregate progress SHALL be `sum(bytesDownloaded) / sum(totalBytesToDownload)` coerced to `0f..1f`. WHEN total is unknown (`<= 0`, e.g. early `PENDING`), THEN it SHALL fall back to the average `transferProgressPercentage / 100`, and WHEN that is also zero, THEN it SHALL preserve the last `Downloading` progress instead of regressing to `0%`.
4. WHEN any pack is `PENDING`, `DOWNLOADING`, or `TRANSFERRING`, THEN THE state SHALL be `Downloading(progress)`.
5. WHEN any pack is `WAITING_FOR_WIFI` or `REQUIRES_USER_CONFIRMATION`, THEN THE state SHALL remain `Downloading(last progress)` and SHALL NOT revert to `Idle`.
6. WHILE a download is in progress, THE ScanCard SHALL display the percentage from `ModelState.Downloading` (e.g. `Downloading <name>... <N>%`).

### Requirement 4: Part Assembly and Path Resolution

**User Story:** As a user, I want the split download to become one usable model file automatically.

#### Acceptance Criteria

1. THE assembly SHALL resolve part `i` as `File(getPackLocation(aiPackNames[i]).assetsPath(), partFileNames[i])`; WHEN any location, assets path, part name, or part file is missing, THEN it SHALL return `null` without throwing.
2. THE `assembleParts` SHALL stream-copy parts in order through a `.tmp` file (8MB buffers), verify `tmp.length() == sum(part.length())`, then `renameTo` (fallback: `copyTo` overwrite + delete tmp); on size mismatch or exception it SHALL delete tmp and return `null`.
3. THE `getModelPath` SHALL return the assembled file path when it exists, else the `tryAssembleSplitModel` result path, else `""`; it SHALL NOT throw.
4. THE `isModelDownloaded` SHALL return `true` when the assembled file exists, else whether all part files exist; `getPackLocation` failure SHALL yield `false`, never throw.

### Requirement 5: Errors and DEBUG Fallback

**User Story:** As a developer, I want local development without Play packs to degrade to Idle instead of a blocking error.

#### Acceptance Criteria

1. WHEN `getPackStates` fails with `-2` or `PACK_UNAVAILABLE`, THEN THE hint SHALL be `"<msg> (pack未配信: .aabにassetPacksが含まれているか、Play Consoleの配布トラックを確認)"`; in `DEBUG` the state SHALL be `Idle`, in release `Error(hint)`.
2. WHEN `fetch` fails or throws, THEN in `DEBUG` the state SHALL be `Idle`, in release `Error("Download trigger failed: <msg>")` or `Error(<msg>)`.
3. WHEN any pack reports `FAILED`, THEN in `DEBUG` the state SHALL be `Idle`, in release `Error("Pack status check failed")`.
4. WHEN packs report `CANCELED`, `NOT_INSTALLED`, `UNKNOWN`, or the snapshot misses a pack, THEN THE state SHALL be `Idle` so the user can retry.
5. THE `Error` state SHALL carry a human-readable message; `Idle` SHALL render "Not Installed" with a Download action.

### Requirement 6: UI Contract for Model Download

**User Story:** As a user, I want clear download, progress, ready, and error states on the extraction screen.

#### Acceptance Criteria

1. THE `ExtractionPreviewScreen` SHALL render `Idle` as "Not Installed" + "Download via Google Play", `Downloading` as a determinate indicator + `"Downloading <name>... <N>%"`, `Ready` as the model-ready extraction entry, and `Error` as the message + "Retry Status Check" (plus the Play-Store dev tip when the message contains `-1`).
2. THE model selector SHALL be disabled WHILE state is `Downloading` or extraction is running.
3. THE `createDummyModelBtn` helper SHALL be `BuildConfig.DEBUG` only: it writes the dummy `filesDir/fileName` and refreshes status; it SHALL NOT exist in release builds.

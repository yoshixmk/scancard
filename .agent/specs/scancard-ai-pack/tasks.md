# Implementation Plan: ScanCard AI Pack Delivery

## Overview

This plan covers the AI-pack delivery slice only: two on-demand packs for Gemma 4 E2B, `ModelManager` lifecycle with listener-driven progress aggregation, part assembly, repository exposure, download UI, and unit tests. All tasks are completed; this file retains the final decided state.

## Task Dependency Graph

1. **Packaging** (1.1 → 1.2): ai-pack modules must exist before the app references them.
2. **SDK & Config** (2.1 → 2.2): ai-delivery integration before `ModelConfig`.
3. **Manager** (3.1 → 3.2 → 3.3 → 3.4): status query before download trigger before listener/progress before assembly.
4. **Exposure & UI** (4.1 → 4.2): repository delegation before screen rendering.
5. **Verification** (5.1): unit tests cover the manager contract.

| Task | Dependencies | Description |
|------|--------------|-------------|
| 1.2 | 1.1 | App `assetPacks` needs both pack modules |
| 2.2 | 2.1 | `ModelConfig` pack names match module pack names |
| 3.2 | 3.1 | `downloadModel` reuses status snapshot handling |
| 3.3 | 3.1, 3.2 | Listener aggregates the snapshots from query/fetch |
| 3.4 | 3.1 | `COMPLETED` triggers assembly attempt |
| 4.1 | 3.1, 3.2 | Repository delegates to manager |
| 4.2 | 4.1 | Screen renders repository state |
| 5.1 | 3.3, 3.4 | Tests verify progress and assembly |

## Tasks

### 1. Pack Packaging

- [x] 1.1 Create `:gemma-ai-pack` (`packName = "gemma_ai_pack"`, on-demand, holds `gemma-4-E2B-it.litertlm.part0`) and `:gemma-ai-pack-2` (`packName = "gemma_ai_pack_2"`, on-demand, holds `gemma-4-E2B-it.litertlm.part1`) with `com.android.ai-pack`
- [x] 1.2 Declare `assetPacks.add(":gemma-ai-pack")` and `assetPacks.add(":gemma-ai-pack-2")` in `app/build.gradle.kts` so the `.aab` contains both packs

### 2. SDK and Model Definition

- [x] 2.1 Integrate `com.google.android.play:ai-delivery:0.2.0-beta01` (`AiPackManager`, `AiPackStateUpdateListener`)
- [x] 2.2 Define `ModelConfig.GEMMA_4_E2B` (2.6GB, `gemma-4-E2B-it.litertlm`, pack/part name lists) as the single `AVAILABLE_MODELS` entry; define `ModelState` (`Idle`, `Downloading(progress)`, `Ready`, `Error`)

### 3. ModelManager Lifecycle

- [x] 3.1 Implement `checkModelStatus` (assembled file → `Ready`; else set active config, ensure single listener, `getPackStates` with direct executor, `handlePackStates`, `PACK_UNAVAILABLE` hint with DEBUG → `Idle`)
- [x] 3.2 Implement `downloadModel` (`fetch` with direct executor, reflect fetch snapshot, re-query; DEBUG → `Idle` on failure)
- [x] 3.3 Implement single `AiPackStateUpdateListener` that re-queries `getPackStates` per callback and `computeAggregateProgress` (`sum(done)/sum(total)`, `transferProgressPercentage` fallback, last-progress preservation; `PENDING`/`DOWNLOADING`/`TRANSFERRING` and `WAITING_FOR_WIFI`/`REQUIRES_USER_CONFIRMATION` → `Downloading`)
- [x] 3.4 Implement `tryAssembleSplitModel` / `assembleParts` (ordered stream copy via `.tmp`, size verification, rename-or-copy, tmp cleanup) plus non-throwing `getModelPath` / `isModelDownloaded`

### 4. Exposure and UI

- [x] 4.1 Implement `ModelRepositoryImpl` (`modelState` delegation, `checkModelStatus`, `downloadModel`, `getModelPath` null-when-missing, `getAvailableModels`)
- [x] 4.2 Implement `ExtractionPreviewScreen` download contract (`Idle` download button, determinate `Downloading N%`, `Ready` extraction entry, `Error` retry; selector disabled while downloading; DEBUG-only `createDummyModelBtn`)

### 5. Verification

- [x] 5.1 Extend `ModelManagerTest` (DEBUG Idle fallbacks, non-throwing `isModelDownloaded`, ordered `assembleParts`, single listener registration, listener `0% → 50%` progression, `TRANSFERRING`/`WAITING_FOR_WIFI` stay `Downloading`, unknown-total progress preservation)

---

## Completed Milestones

- Two on-demand AI Packs deliver Gemma 4 E2B within the Play 1.5GB/pack limit.
- Listener-driven progress aggregation ends the stuck-at-0% download UI.
- Split parts assemble into a single size-verified model file before inference.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` passes.

# Appium E2E

Run E2E for the Android Compose app with Appium (WebdriverIO + UiAutomator2).

## Structure

```
appium/
├── wdio.conf.js          # Capabilities / Appium service / virtualScene hooks
├── wdio.slow.conf.js     # @slow only (extends wdio.conf.js)
├── package.json          # @wdio/cli + appium
├── helpers/
│   ├── utils.js          # clearStateAndLaunch(), tapByTestTag(), waitVisible()
│   └── virtualScene.js   # Virtual Scene image injection (adb emu virtualscene-image)
├── images/
│   └── sample.jpg        # Virtual Scene source (host path, wall default)
├── pageobjects/
│   ├── HomePage.js
│   ├── DeckDetailPage.js
│   ├── ScanPage.js
│   ├── StudyPage.js
│   └── ExportPage.js
└── specs/
    ├── home.e2e.js
    ├── scanToExport.e2e.js
    ├── scanPostPhoto.e2e.js        # Req22 black-screen fix
    ├── directScanLaunch.e2e.js     # Req23 direct camera (no Start Scanning tap)
    ├── studyFilter.e2e.js
    ├── studyAndResume.e2e.js
    ├── fastFlow.e2e.js             # @slow Fast Mode
    ├── deckValidation.e2e.js
    ├── cardDisappearAfterClose.e2e.js
    └── backgroundExtraction.e2e.js # @slow WorkManager
```

Compose uses `Modifier.testTag()` and Appium reaches it via:

- `~<testTag>` (accessibility id)
- `android=new UiSelector().resourceId("{APP_ID}:id/<testTag>")`
- fallback: `text` / `description` selectors (tried automatically by `tapByTestTag`)

Main testTags:

| Screen | tag |
|--------|-----|
| Home | `homeFabScan`, `homeCreateDeckBtn`, `newDeckTitleInput`, `newDeckCreateBtn`, `deckCard_<title>`, `deckDeleteBtn_<title>` |
| DeckDetail | `deckDetailFabAddCard`, `deckDetailStudyBtn`, `deckDetailExportBtn`, `deckDetailCardCount`, `addCardScanOption`, `addCardManualOption`, `cardItem_<id>`, `cardStatus_<id>_<STATUS>` |
| Scan | `scanOpeningIndicator` (Req23), `scanStartBtn` (fallback only), `scanRetryBtn`, `scanThumb_<id>`, `scanExtractBtn`, `scanAddMoreBtn`, `scanDoneBtn`, `scanDummyInsertBtn` (DEBUG) |
| Study | `studyFilterBtn`, `filterItem_<ALL|LEARNING|REVIEW|NEW>`, `studyEmpty`, `studyCard`, `studyAgainBtn`/`studyGoodBtn` |
| Export | `exportChipTSV`, `exportChipCSV`, `exportPreviewText`, `exportCopyBtn`, `exportShareBtn` |
| Extraction | `extractionStartBtn`, `createDummyModelBtn` (DEBUG) |

## Prerequisites

- Node.js 20.x (via mise)
- Android SDK / Emulator (API 36 google_apis_playstore, `hw.camera.back=virtualscene`, minSdk 26)
- JDK 17+ (current: JDK 25) + Gradle 9.x + AGP 9.3.1 — `./gradlew :app:assembleDebug`
- Start emulator: `emulator -avd Medium_Phone_API_36.0 -port 5554 &` (or `ANDROID_DEVICE=emulator-5554`)
- `appium` server is auto-started via wdio `services: [appium]` (`relaxedSecurity: true`)

## Virtual Scene (Camera)

Manual import via Android Studio is not required. `wdio.conf.js:67` `onPrepare` injects `appium/images/*.{png,jpg}` via host-side `adb emu virtualscene-image` (`helpers/virtualScene.js:34`).

- `wall` default: `sample.jpg` (filename contains `wall` → wall, `table` → table, otherwise wall)
- Override dir: `VIRTUAL_SCENE_IMAGES_DIR=/path/to/images npm test`
- Override serial: `ANDROID_SERIAL=emulator-5554` / `ANDROID_DEVICE`
- Reset: `wdio.conf.js:73` `onComplete` restores `wall`/`table` to defaults (`virtualScene.js:63` `resetVirtualSceneImages()`); `afterTest` keeps images for speed (uncomment reset for per-test isolation)
- Requires `hw.camera.back=virtualscene` (`config.ini:17`); command verified: `adb -s emulator-5554 emu help virtualscene-image` → `Usage: virtualscene-image <wall|table> [path]`

## Install

```bash
cd appium
mise exec node@20 -- npm install
mise exec node@20 -- npx appium driver install uiautomator2
```

## Run

```bash
# via mise (recommended, Node 20)
mise exec node@20 -- npm test                 # all specs (excludes @slow)
mise exec node@20 -- npm run test:slow       # @slow only (wdio.slow.conf.js)
mise exec node@20 -- npm run test:home
mise exec node@20 -- npm run test:scan-export
mise exec node@20 -- npm run test:filter
mise exec node@20 -- npm run test:fast-flow  # Fast Mode @slow

# single spec
mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/directScanLaunch.e2e.js
mise exec node@20 -- npx wdio run wdio.slow.conf.js --spec specs/fastFlow.e2e.js

# override via env vars
APK_PATH=../app/build/outputs/apk/debug/app-debug.apk APP_ID=com.plath.scancard ANDROID_DEVICE=emulator-5554 mise exec node@20 -- npm test
VIRTUAL_SCENE_IMAGES_DIR=./images mise exec node@20 -- npm test
```

`wdio.conf.js:58` `grep: '@slow', invert: true` excludes manual-verification equivalents from default; `wdio.slow.conf.js:1` clears grep to run only slow.

## Selectors

- `tapByTestTag("homeFabScan", {fallbackText:"Scan Document"})` tries `~tag` -> `resourceId` -> `description` -> `text` in order
- `waitVisible('android=new UiSelector().text("Text")', timeout)`
- `pressBack()` to navigate back
- Virtual Scene: `setupVirtualSceneFromDir(dir)` / `setVirtualSceneImage('wall', absPath)` / `resetVirtualSceneImages()` in `helpers/virtualScene.js`

## CI Example (GitHub Actions)

```yaml
- uses: actions/setup-node@v4  # or mise
- run: cd appium && mise exec node@20 -- npm ci
- uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 36
    target: google_apis_playstore
    arch: x86_64
    script: |
      ./gradlew :app:assembleDebug
      cd appium && mise exec node@20 -- npm test
      cd appium && mise exec node@20 -- npm run test:slow
```

## Troubleshooting

- `resourceId not found` → search via `~tag` and fallback text. Verify with Appium Inspector.
- `GmsDocumentScanner` overlay → dismiss with `pressBack()` / `input keyevent 4`. Req23: `scanOpeningIndicator` + `Opening camera...` shown, `scanStartBtn` only on `scannerError`/`RESULT_CANCELED`.
- `noReset` keeps data → `wdio.conf.js` uses `clearApp` per test (`helpers/utils.js:16`). Use `noReset:true` only for persistence checks.
- Virtual Scene not showing → check `adb -s emulator-5554 emu virtualscene-image wall` returns `OK`, `config.ini` has `hw.camera.back=virtualscene`, image path is host-absolute and PNG/JPEG, emulator and `adb` on same host.
- `@slow` not running → use `npm run test:slow` (`wdio.slow.conf.js`), not `npm test`.

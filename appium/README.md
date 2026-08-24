# Appium E2E

Run E2E for the Android Compose app with Appium (WebdriverIO + UiAutomator2).

## Structure

```
appium/
├── wdio.conf.js          # Capabilities / Appium service
├── package.json          # @wdio/cli + appium
├── helpers/
│   ├── caps.js           # APP_ID / APK_PATH
│   └── utils.js          # clearStateAndLaunch(), tapByTestTag(), waitVisible()
├── pageobjects/
│   ├── HomePage.js
│   ├── DeckDetailPage.js
│   ├── ScanPage.js
│   ├── StudyPage.js
│   └── ExportPage.js
└── specs/
    ├── home.e2e.js
    ├── scanToExport.e2e.js
    └── studyFilter.e2e.js
```

Compose uses `Modifier.testTag()` and Appium reaches it via:

- `~<testTag>` (accessibility id)
- `android=new UiSelector().resourceId("{APP_ID}:id/<testTag>")`
- fallback: `text` / `description` selectors (tried automatically by `tapByTestTag`)

Main testTags:

| Screen | tag |
|--------|-----|
| Home | `homeFabScan`, `homeCreateDeckBtn`, `newDeckTitleInput`, `newDeckCreateBtn`, `deckCard_<title>`, `deckDeleteBtn_<title>` |
| DeckDetail | `deckDetailFabAddCard`, `deckDetailStudyBtn`, `deckDetailExportBtn`, `deckDetailCardCount`, `addCardScanOption`, `addCardManualOption`, `cardItem_<id>` |
| Scan | `scanStartBtn`, `scanExtractBtn`, `scanAddMoreBtn`, `scanDoneBtn` |
| Study | `studyFilterBtn`, `filterItem_<ALL|LEARNING|REVIEW|NEW>`, `studyEmpty` |
| Export | `exportChipTSV`, `exportChipCSV`, `exportPreviewText`, `exportCopyBtn`, `exportShareBtn` |
| Extraction | `extractionStartBtn` |

## Prerequisites

- Node.js (example: 20.x, managed via mise)
- Android SDK / Emulator (API 34+ recommended, minSdk 26)
- Build once: `./gradlew :app:assembleDebug` (`{JAVA_HOME}` example: JDK 17+)
- Start emulator: `emulator -avd {AVD_NAME} &` (example: Medium_Phone)
- `appium` server is auto-started via wdio `services: [appium]`

## Install

```bash
cd appium
mise exec node@20 -- npm install
mise exec node@20 -- npx appium driver install uiautomator2
```

## Run

```bash
# via mise (recommended, Node 20)
mise exec node@20 -- npm test                 # all specs
mise exec node@20 -- npm run test:home
mise exec node@20 -- npm run test:scan-export
mise exec node@20 -- npm run test:filter

# override via env vars
APK_PATH={APK_PATH} APP_ID={APP_ID} ANDROID_DEVICE={DEVICE} mise exec node@20 -- npm test
```

## Selectors

- `tapByTestTag("homeFabScan", {fallbackText:"Scan Document"})` tries `~tag` -> `resourceId` -> `description` -> `text` in order
- `waitVisible('android=new UiSelector().text("Text")', timeout)`
- `pressBack()` to navigate back

## CI Example (GitHub Actions)

```yaml
- uses: actions/setup-node@v4  # or mise
- run: cd appium && mise exec node@20 -- npm ci
- uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: {API_LEVEL}  # example: 34
    target: google_apis
    script: |
      ./gradlew :app:assembleDebug
      cd appium && mise exec node@20 -- npm test
```

## Troubleshooting

- `resourceId not found` → search via `~tag` and fallback text. Verify with Appium Inspector.
- `GmsDocumentScanner` overlay → dismiss with `pressBack()`. Use `FakeDocumentScanner` via `Hilt TestModule` for deterministic tests.
- `noReset` keeps data → `wdio.conf.js` uses `clearApp` per test. Use `noReset:true` only for persistence checks.

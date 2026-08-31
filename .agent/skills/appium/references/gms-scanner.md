# GMS DocumentScanner External UI

`GmsDocumentScanning.getClient().getStartScanIntent()` launches `com.google.android.gms`.

- Detect: `await driver.getCurrentPackage()` → if `includes('gms')` then handle in priority: `Discard` button → `Retake page` (back) → `Next`/`Save` → `pressKeyCode(4)`. Covers `Discard document?` confirmation after back.
- `activateApp` does not pop back stack; use `pressBack()` or `clearStateAndLaunch()` to reset.
- For E2E without scanner: use `BuildConfig.DEBUG` dummy (`scanDummyInsertBtn`, `files/gemma-4-E2B-it.litertlm` <5MB) as in `specs/backgroundExtraction.e2e.js`.

**CI**: Scanner is flaky; keep scan flow optional and reset via `clearStateAndLaunch()`.

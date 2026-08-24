# GMS DocumentScanner External UI

`GmsDocumentScanning.getClient().getStartScanIntent()` launches an external `com.google.android.gms` activity.

- Detect: `await driver.getCurrentPackage()` → if `includes('gms')` then `await driver.pressKeyCode(4)`.
- `activateApp` does not pop back stack; use `pressBack()` or `clearStateAndLaunch()` to reset.

**CI**: Scanner is flaky (Play Services + camera). Keep FAB/scan flow in `try/catch` optional and reset via `clearStateAndLaunch()` before assertions (e.g. `home.e2e.js`).

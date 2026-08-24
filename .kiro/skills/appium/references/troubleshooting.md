# Troubleshooting (ScanCard-specific only)

| Symptom | Fix |
|---|---|
| `tapByTestTag failed` | Pass `fallbackText` matching `contentDescription` (resource-id is empty for Compose) |
| Permission dialog `While using the app` covers UI | `pm grant CAMERA` + `handlePermissionDialog()` |
| `getCurrentPackage()==com.google.android.gms` after FAB | `pressBack()`; keep scan optional via `try/catch` |
| `Home not found after back` | `activateApp` does not pop stack; use `clearStateAndLaunch()` |

Generic Appium/Android issues (`ANDROID_HOME`, `platformVersion`, `emulator offline`, JDK) are omitted — inferable from tool output.

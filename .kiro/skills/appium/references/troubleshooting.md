# Troubleshooting (ScanCard-specific only)

| Symptom | Fix |
|---|---|
| `tapByTestTag failed` | Pass `fallbackText` matching `contentDescription` |
| Permission dialog covers UI | `pm grant CAMERA+POST_NOTIFICATIONS` + `handlePermissionDialog()` |
| `getCurrentPackage()==com.google.android.gms` | Tap `Discard` first, then `pressBack()`; keep scan optional |
| `Home not found after back` | `activateApp` does not pop stack; use `clearStateAndLaunch()` |
| `InvalidForegroundServiceTypeException` or `type none prohibited` | Add `<service SystemForegroundService foregroundServiceType="shortService">` merge + grant `POST_NOTIFICATIONS` |
| `ScanCard keeps stopping` after extraction start | Same foreground fix; check `logcat -s AndroidRuntime` |
| Dummy `scanDummyInsertBtn` not found | Ensure `BuildConfig.DEBUG` build and emulator with `Memory 6144` |

Generic Appium/Android issues (`ANDROID_HOME`, `platformVersion`, `emulator offline`, JDK) are omitted — inferable from tool output.

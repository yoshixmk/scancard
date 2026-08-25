# Permission Dialogs

`ScanScreen` auto-requests `CAMERA`; `CardExtractionWorker` needs `POST_NOTIFICATIONS` for foreground notification (targetSDK 35).

`mobile: clearApp` wipes `autoGrantPermissions`, so dialogs reappear after each `clearStateAndLaunch()`.

**Mitigation** (`helpers/utils.js:clearStateAndLaunch` + `handlePermissionDialog`):

1. `pm grant CAMERA` and `pm grant POST_NOTIFICATIONS` via `mobile: shell`.
2. UI fallback: click `While using the app` / `Only this time` if displayed.

Call `handlePermissionDialog()` after `activateApp`.

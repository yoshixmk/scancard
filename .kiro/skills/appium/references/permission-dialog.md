# CAMERA Permission Dialog

`ScanScreen` auto-requests `CAMERA`; on API 36 the system dialog `Allow ScanCard to take pictures... / While using the app` appears.

`mobile: clearApp` wipes `autoGrantPermissions`, so dialog reappears after each `clearStateAndLaunch()`.

**Mitigation** (in `helpers/utils.js:clearStateAndLaunch` + `handlePermissionDialog`):

1. `driver.execute('mobile: shell', {command:'pm', args:['grant', APP_ID, 'android.permission.CAMERA']})`
2. UI fallback: click `While using the app` / `Only this time` if displayed.

Call `handlePermissionDialog()` after `activateApp` and after FAB taps.

---
name: appium
description: E2E testing with Appium + WebdriverIO for Android Compose apps. Handles UiAutomator2, Compose testTag, permission dialogs, GMS scanner overlay, and mise-managed Node. Use when creating, running, or debugging Appium E2E suites.
license: Complete terms in LICENSE.txt
metadata:
  author: ScanCard
  last-updated: '2026-08-24'
  keywords:
  - appium
  - webdriverio
  - uiautomator2
  - e2e
  - compose
  - mise
---

# Appium E2E Specialist

E2E for 100% Compose app via WebdriverIO + UiAutomator2.

## When to use

- New or flaky E2E (selector, permission dialog, GMS overlay)
- Appium/Node version mismatch (`p-limit` ESM cycle)

## Quick Start

```powershell
& $mise exec node@20 -- npm install
& $mise exec node@20 -- npx appium driver install uiautomator2
$env:ANDROID_HOME="{ANDROID_SDK}"; & $mise exec node@20 -- npx wdio run wdio.conf.js
```

`$mise` = `{MISE_BIN}`. Full automation: `scripts/setup.ps1` (requires JDK 17+, running emulator).

## ScanCard-specific constraints

1. **testTag not exposed** — `Modifier.testTag()` is invisible to UIAutomator (`resource-id=""`). Use `helpers/utils.js:tapByTestTag(tag, {fallbackText})` which falls back to `contentDescription`/`text` → `references/compose-testtag.md`.
2. **clearApp wipes grant** — `autoGrantPermissions:true` is cleared by `mobile: clearApp`. `clearStateAndLaunch()` re-grants via `pm grant CAMERA` and clicks `While using the app` via `handlePermissionDialog()` → `references/permission-dialog.md`.
3. **GMS overlay** — `GmsDocumentScanning.getStartScanIntent()` launches `com.google.android.gms`. Detect `getCurrentPackage()` then `pressBack()`; make scan optional for CI → `references/gms-scanner.md`.
4. **Node 20 required** — Appium 3.x + uiautomator2 fails on Node 22/24 (`p-limit` ESM cycle). Always `mise exec node@20 -- <cmd>` → `references/mise-node.md`.

## Troubleshooting (non-obvious only)

| Symptom | Fix |
|---|---|
| `tapByTestTag failed: <tag>` | Pass `fallbackText` matching `contentDescription` |
| Permission dialog covers UI | `pm grant` + `handlePermissionDialog()` clicks `While using the app` |
| `getCurrentPackage()==com.google.android.gms` after FAB | `pressBack()`; keep scan `try/catch` optional |

See `references/troubleshooting.md` for details.

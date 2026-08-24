# Execution

```powershell
$env:ANDROID_HOME="{ANDROID_SDK}"; & $mise exec node@20 -- npx wdio run wdio.conf.js          # all specs
& $mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/home.e2e.js  # single spec
```

Verified on `{AVD_NAME}` (Android 16, API 36): `home`, `studyFilter`, `scanToExport` PASS when using Node 20 and fallbacks above. Scanner flow optional for CI.

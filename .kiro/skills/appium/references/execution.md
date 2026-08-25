# Execution

```powershell
$env:ANDROID_HOME="{ANDROID_SDK}"; & $mise exec node@20 -- npx wdio run wdio.conf.js          # all specs
& $mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/home.e2e.js  # single spec
& $mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/backgroundExtraction.e2e.js  # foreground Worker (dummy model <5MB)
```

Verified on `{AVD_NAME}` (Android 16, API 36): `home`, `studyFilter`, `scanToExport`, `backgroundExtraction` PASS with Node 20 and `pm grant CAMERA+POST_NOTIFICATIONS`. `backgroundExtraction` uses `BuildConfig.DEBUG` dummy scan/model to avoid GMS and 2.6GB asset.

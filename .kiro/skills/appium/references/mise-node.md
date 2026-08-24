# mise + Node Version

Appium 3.x + uiautomator2 fails on Node 22/24:

`Cannot require() ES Module p-limit/... in a cycle` / `p_limit_1.limitFunction is not a function`

**Fix**: Use Node 20 via mise:

```powershell
& $mise exec node@20 -- npm install
& $mise exec node@20 -- npx appium driver install uiautomator2
& $mise exec node@20 -- npx wdio run wdio.conf.js
```

Do not use bare `npx wdio` (may pick newer Node from PATH). `$mise` = `{MISE_BIN}`.

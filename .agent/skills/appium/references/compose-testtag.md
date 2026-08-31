# Compose testTag vs UIAutomator

`Modifier.testTag("homeFabScan")` is not exposed as `resource-id` to UIAutomator2 (Compose stores it in `SemanticsPropertyKey.TestTag`, only visible to `ComposeTestRule`). `uiautomator dump` shows `resource-id=""`, `content-desc="Scan Document"`.

**Rule**: Keep `testTag` + `contentDescription`, and always call with fallback:

```js
await tapByTestTag('homeFabScan', {fallbackText:'Scan Document'});
// tries: ~tag → resourceId → description(tag) → description(fallbackText) → text(fallbackText)
```

Do not rely on `resourceId("com.plath.scancard:id/<tag>")` alone; it returns `NoSuchElement` without `testTagsAsResourceId`.

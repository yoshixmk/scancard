/**
 * Appium helper utilities for ScanCard
 * Provides clearState, wait helpers, and resilient selectors (testTag > accessibility id > text)
 */

export const APP_ID = process.env.APP_ID || 'com.plath.scancard';

/**
 * Clear app data and launch (clean state). Uses mobile:clearApp + activateApp.
 */
export async function clearStateAndLaunch() {
    try {
        // Prefer Appium extension: clearApp (wipes data)
        await driver.execute('mobile: clearApp', { appId: APP_ID });
    } catch {
        try { await driver.terminateApp(APP_ID); } catch {}
        // optional: adb shell pm clear via mobile:shell for full reset
        try {
            await driver.execute('mobile: shell', {
                command: 'pm',
                args: ['clear', APP_ID]
            });
        } catch {}
    }
    await driver.activateApp(APP_ID);
    // Pre-grant CAMERA to avoid system permission dialog (ScanScreen requests CAMERA)
    try {
        await driver.execute('mobile: shell', {
            command: 'pm grant ' + APP_ID + ' android.permission.CAMERA',
            args: []
        });
    } catch {
        try {
            await driver.execute('mobile: shell', {
                command: 'pm',
                args: ['grant', APP_ID, 'android.permission.CAMERA']
            });
        } catch {}
    }
    // Small pause for app to settle
    await driver.pause(800);
    await handlePermissionDialog();
}

export async function handlePermissionDialog(timeoutMs = 2000) {
    const dialogs = [
        'android=new UiSelector().text("While using the app")',
        'android=new UiSelector().text("Only this time")',
        'android=new UiSelector().text("Allow")',
        'android=new UiSelector().textContains("Allow")',
    ];
    for (const sel of dialogs) {
        try {
            const el = await $(sel);
            if (await el.isDisplayed().catch(() => false)) {
                await el.click();
                await driver.pause(500);
                return true;
            }
        } catch {}
    }
    return false;
}

/**
 * Robust wait for element visible. Supports ~ accessibility id (testTag fallback) and text.
 * @param {string} selector - WebdriverIO selector, e.g. '~homeFabScan' or 'android=new UiSelector().text("...")'
 * @param {number} timeoutMs
 */
export async function waitVisible(selector, timeoutMs = 8000) {
    const el = await $(selector);
    await el.waitForDisplayed({ timeout: timeoutMs });
    return el;
}

/**
 * Tap by testTag (Compose testTag -> resource-id = "<package>:id/<tag>") or by accessibility id.
 * Appium UIAutomator2 exposes testTag as accessibility id when using Modifier.testTag()
 * If tag not found, falls back to text search.
 */
export async function tapByTestTag(tag, { fallbackText } = {}) {
    // Compose testTag is exposed as `resource-id` with suffix, but WebdriverIO `~` searches content-desc/accessibility
    // UIAutomator2 also exposes testTag via `description` in some Compose versions, so try both
    const selectors = [
        `~${tag}`,
        `android=new UiSelector().resourceId("${APP_ID}:id/${tag}")`,
        `android=new UiSelector().resourceIdMatches(".*:id/${tag}")`,
        `android=new UiSelector().description("${tag}")`,
    ];
    if (fallbackText) {
        selectors.push(`android=new UiSelector().description("${fallbackText}")`);
        selectors.push(`android=new UiSelector().descriptionContains("${fallbackText}")`);
        selectors.push(`android=new UiSelector().text("${fallbackText}")`);
        selectors.push(`android=new UiSelector().textContains("${fallbackText}")`);
    }
    for (const sel of selectors) {
        try {
            const el = await $(sel);
            if (await el.isDisplayed().catch(() => false)) {
                await el.click();
                return el;
            }
        } catch {}
    }
    throw new Error(`tapByTestTag failed: ${tag} (fallbackText=${fallbackText})`);
}

/**
 * Input text into EditText: finds by testTag first, then by className
 */
export async function inputByTestTag(tag, text, fallbackHint) {
    let el;
    try {
        el = await waitVisible(`~${tag}`, 5000);
    } catch {
        el = await waitVisible('android=new UiSelector().className("android.widget.EditText")', 5000);
        if (fallbackHint) {
            // try hint fallback
            try {
                const hintEl = await $(`android=new UiSelector().textContains("${fallbackHint}")`);
                if (await hintEl.isDisplayed().catch(() => false)) el = hintEl;
            } catch {}
        }
    }
    await el.click();
    await el.setValue(text);
    return el;
}

/**
 * Wait for text visible / notVisible
 */
export async function waitForText(text, { timeout = 5000, notVisible = false } = {}) {
    const sel = `android=new UiSelector().text("${text}")`;
    const el = await $(sel);
    if (notVisible) {
        await el.waitForDisplayed({ timeout, reverse: true });
    } else {
        await el.waitForDisplayed({ timeout });
    }
    return el;
}

export async function pressBack() {
    await driver.pressKeyCode(4); // KEYCODE_BACK
}

export async function hideKeyboardIfShown() {
    try {
        if (await driver.isKeyboardShown()) await driver.hideKeyboard();
    } catch {}
}

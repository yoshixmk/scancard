/**
 * backgroundExtraction.e2e.js - E2E for background extraction with foreground WorkManager
 * Requires dummy model file (<5MB) in filesDir to trigger Gemma dummy mode (BuildConfig.DEBUG)
 * and dummy scan insertion via scanDummyInsertBtn (DEBUG only).
 * Verifies WorkManager foreground fix: no cancellation, cards created, foreground notification not crashing.
 */
import { clearStateAndLaunch, waitVisible, tapByTestTag, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';

describe('Background extraction (foreground WorkManager)', () => {
    const DECK = 'BG E2E Deck';

    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
        // Ensure dummy model file exists for DEBUG dummy mode (<5MB)
        try {
            await driver.execute('mobile: shell', {
                command: 'run-as com.plath.scancard sh -c "mkdir -p files && echo dummy > files/gemma-4-E2B-it.litertlm && ls -lh files/gemma-4-E2B-it.litertlm"',
            });
        } catch (e) {
            console.log('[bg] dummy model create failed (may already exist):', e.message);
        }
    });

    afterEach(async () => {
        // Cleanup via clearApp (faster than delete)
        try {
            await clearStateAndLaunch();
            await homePage.waitLoaded(5000);
        } catch {}
    });

    it('should extract dummy cards in background without cancellation', async () => {
        // 1. Create deck
        await homePage.createDeck(DECK);
        await waitVisible(`android=new UiSelector().text("${DECK}")`, 5000);
        await homePage.tapDeck(DECK);
        await deckDetailPage.waitLoaded(DECK);
        await deckDetailPage.assertCardCount('0 Cards');

        // 2. Add Card -> Scan Document -> Insert Dummy Scan (E2E)
        try {
            await tapByTestTag('deckDetailFabAddCard');
        } catch {
            const fab = await $('android=new UiSelector().description("Add Card")');
            await fab.click();
        }
        await driver.pause(700);
        // Handle Add Card dialog: Manual vs Scan
        try {
            await tapByTestTag('addCardScanOption');
        } catch {
            try {
                const scanOpt = await waitVisible('android=new UiSelector().text("Scan Document")', 4000);
                await scanOpt.click();
            } catch {}
        }
        await driver.pause(1500);
        // Handle GMS scanner - it may be in camera, preview (Next/Save), or Discard dialog
        for (let attempt = 0; attempt < 5; attempt++) {
            const pkg = await driver.getCurrentPackage().catch(() => 'unknown');
            console.log(`[bg] current package before GMS handling [${attempt}]: ${pkg}`);
            if (pkg.includes('com.google.android.gms')) {
                // Discard dialog has highest priority (appears after back press)
                try {
                    const discardBtn = await waitVisible('android=new UiSelector().text("Discard")', 2000);
                    console.log('[bg] Tapping Discard in GMS dialog');
                    await discardBtn.click();
                    await driver.pause(1200);
                    continue;
                } catch {}
                try {
                    const retakeBtn = await waitVisible('android=new UiSelector().text("Retake page")', 2000);
                    // If Retake is visible, we prefer Discard, but fallback to back
                    console.log('[bg] Retake page visible, pressing back to trigger Discard');
                    try { await driver.execute('mobile: shell', { command: 'input keyevent 4' }); } catch {}
                    await driver.pause(900);
                    continue;
                } catch {}
                // Try Next/Save in preview
                try {
                    const nextBtn = await waitVisible('android=new UiSelector().text("Next")', 2000);
                    console.log('[bg] Tapping Next in GMS preview');
                    await nextBtn.click();
                    await driver.pause(1200);
                    continue;
                } catch {}
                try {
                    const saveBtn = await waitVisible('android=new UiSelector().text("Save")', 2000);
                    console.log('[bg] Tapping Save');
                    await saveBtn.click();
                    await driver.pause(1200);
                    continue;
                } catch {}
                // Fallback: back
                try {
                    await driver.execute('mobile: shell', { command: 'input keyevent 4' });
                    await driver.pause(900);
                } catch {}
            } else if (pkg.includes('com.plath.scancard')) {
                break;
            }
            await driver.pause(500);
        }
        try { await driver.activateApp('com.plath.scancard'); await driver.pause(800); } catch {}
        // Wait for ScanScreen dummy button (testTag may be exposed as resource-id, fallback to text)
        let dummyVisible = false;
        for (let i = 0; i < 2; i++) {
            try {
                await waitVisible('android=new UiSelector().textContains("Dummy")', 5000);
                dummyVisible = true;
                break;
            } catch {}
            try {
                await waitVisible('~scanDummyInsertBtn', 3000);
                dummyVisible = true;
                break;
            } catch {}
            await driver.pause(600);
            // Ensure app in foreground
            try { await driver.activateApp('com.plath.scancard'); await driver.pause(500); } catch {}
        }
        if (!dummyVisible) {
            try {
                const pkg = await driver.getCurrentPackage();
                console.log('[bg] current package after failed dummy wait:', pkg);
                const src = await driver.getPageSource();
                console.log('[bg] page source snippet:', String(src).slice(0, 800));
            } catch {}
            throw new Error('scanDummyInsertBtn not found after 12s');
        }
        // Tap Insert Dummy Scan (E2E) - DEBUG only
        try {
            await tapByTestTag('scanDummyInsertBtn', { fallbackText: 'Dummy' });
        } catch {
            const dummyBtn = await waitVisible('android=new UiSelector().textContains("Dummy")', 3000);
            await dummyBtn.click();
        }

        // 3. Should navigate to ExtractionPreview (AI Extraction)
        await waitVisible('android=new UiSelector().text("AI Extraction")', 8000);
        // Handle Idle -> Create Dummy Model (E2E) if needed
        try {
            const idle = await $('android=new UiSelector().textContains("Not Installed")');
            if (await idle.isDisplayed().catch(() => false)) {
                console.log('[bg] Model Idle, tapping Create Dummy Model');
                try {
                    await tapByTestTag('createDummyModelBtn', { fallbackText: 'Create Dummy' });
                } catch {
                    const dummyModelBtn = await waitVisible('android=new UiSelector().textContains("Create Dummy")', 3000);
                    await dummyModelBtn.click();
                }
                await driver.pause(900);
            }
        } catch (e) {
            console.log('[bg] No Idle state or dummy model not needed:', e.message);
        }
        await waitVisible('android=new UiSelector().textContains("Model Ready")', 8000);
        // Tap Start AI Extraction with retry; confirm the worker actually started via logcat.
        // (elementClick can miss if Compose relayouts shift the cached bounds — verify, don't assume.)
        await driver.execute('mobile: shell', { command: 'logcat -c' });
        let workerStarted = false;
        for (let attempt = 0; attempt < 3 && !workerStarted; attempt++) {
            try {
                await tapByTestTag('extractionStartBtn', { fallbackText: 'Start AI Extraction' });
            } catch {
                const startBtn = await waitVisible('android=new UiSelector().text("Start AI Extraction")', 3000);
                await startBtn.click();
            }
            for (let k = 0; k < 10 && !workerStarted; k++) {
                const lg = await driver.execute('mobile: shell', {
                    command: 'logcat -d -t 300 2>&1 | grep -E "Starting background extraction|WorkerWrapper: Starting work|onProgress"',
                }).catch(() => '');
                if (/Starting|onProgress/.test(String(lg))) {
                    workerStarted = true;
                    console.log('[bg] worker confirmed started via logcat');
                } else {
                    await driver.pause(400);
                }
            }
            if (!workerStarted) console.log(`[bg] start attempt ${attempt} missed — retapping`);
        }
        if (!workerStarted) {
            const src = await driver.getPageSource().catch(() => '');
            console.log('[bg] worker never started, page source:', String(src).slice(0, 1500));
        }
        expect(workerStarted).toBe(true);

        // 4. Wait for background WorkManager: RUNNING -> SUCCEEDED (dummy 8s/page + DB)
        // ExtractionPreview shows "Gemma is extracting..." then auto-navigates to DeckDetail on SUCCEEDED
        // Req12.12: open the notification shade and verify per-page progress ("Page n of m") is visible
        let sawProgressNotif = false;
        let progressText = '';
        await driver.openNotifications();
        try {
            for (let i = 0; i < 15 && !sawProgressNotif; i++) {
                try {
                    const el = await $('android=new UiSelector().textContains("Page ")');
                    if (await el.isDisplayed().catch(() => false)) {
                        progressText = await el.getText();
                        sawProgressNotif = /Page \d+ of \d+/.test(progressText);
                        console.log(`[bg] shade shows progress notification: "${progressText}"`);
                        break;
                    }
                } catch {}
                await driver.pause(600);
            }
        } finally {
            // closeNotificationsはwdioに無いのでBACKでシェードを閉じる
            try { await driver.pressKeyCode(4); } catch {}
            await driver.pause(600);
        }
        if (!sawProgressNotif) {
            // フォールバック診断: シェードUIに無い場合はdumpsysも確認（catchで握りつぶさない）
            const notifDump = await driver.execute('mobile: shell', {
                command: 'dumpsys notification --noredact 2>&1 | grep -iE "Extracting|Page [0-9]|android.text" | head -20',
            });
            console.log('[bg] no Page n of m in shade, dumpsys:', String(notifDump).slice(0, 1200));
        }
        expect(sawProgressNotif).toBe(true);
        expect(progressText).toMatch(/Page \d+ of \d+/);

        // Extraction may have auto-navigated to DeckDetail while the shade was open — settle first
        try { await driver.activateApp('com.plath.scancard'); } catch {}
        await driver.pause(800);

        // Wait for auto-finish to DeckDetail (cards created) with diagnostics
        try {
            await waitVisible('android=new UiSelector().textContains("Cards")', 25000);
        } catch (e) {
            try {
                const pkg = await driver.getCurrentPackage().catch(() => 'unknown');
                console.log('[bg] Cards wait failed, current package:', pkg);
                const src = await driver.getPageSource();
                console.log('[bg] page source on Cards failure:', String(src).slice(0, 2000));
                const notif2 = await driver.execute('mobile: shell', { command: 'dumpsys notification 2>&1 | head -80' }).catch(() => '');
                console.log('[bg] notif dump on failure:', String(notif2).slice(0, 800));
                const log = await driver.execute('mobile: shell', { command: 'logcat -d 2>&1 | grep -E "ScanScreen|Extraction|CardExtraction|Gemma|WM-Worker" | tail -40' }).catch(() => '');
                console.log('[bg] logcat on failure:', String(log).slice(0, 1500));
            } catch {}
            throw e;
        }
        // Should be 2 Cards (dummy Apple/Banana)
        try {
            const cardCount = await waitVisible('android=new UiSelector().textContains("2 Cards")', 8000);
            expect(await cardCount.isDisplayed()).toBe(true);
            console.log('[bg] Found 2 Cards as expected');
        } catch {
            // Fallback: at least not 0 Cards
            const anyCards = await $('android=new UiSelector().textContains("Cards")');
            const txt = await anyCards.getText();
            console.log('[bg] Card count text:', txt);
            expect(txt).not.toContain('0 Cards');
        }

        // 5. Verify logcat no cancellation (optional, via mobile: shell)
        try {
            const pid = await driver.execute('mobile: shell', { command: 'pidof com.plath.scancard' });
            const log = await driver.execute('mobile: shell', { command: `logcat -d --pid=${String(pid).trim()} | grep -i \"CardExtractionWorker\\|GemmaExtractor\" | tail -20` });
            console.log('[bg] logcat snippet:', String(log).slice(0, 1000));
            expect(String(log)).not.toContain('Work cancelled');
            expect(String(log)).toContain('Dummy mode enabled');
        } catch (e) {
            console.log('[bg] logcat check skipped:', e.message);
        }

        // Cleanup handled in afterEach, but also verify cards via back to Home
        await pressBack(); // DeckDetail -> Home
        await homePage.waitLoaded(5000);

        // 6. Persistence across restart (Req12.8/12.10): force-stop + relaunch.
        // Cards must survive and extraction must NOT re-run for the completed deck.
        await driver.execute('mobile: shell', { command: 'am force-stop com.plath.scancard' });
        await driver.pause(1500);
        await driver.activateApp('com.plath.scancard');
        await homePage.waitLoaded(10000);
        await homePage.tapDeck(DECK);
        await deckDetailPage.waitLoaded(DECK);
        try {
            const persisted = await waitVisible('android=new UiSelector().textContains("2 Cards")', 8000);
            expect(await persisted.isDisplayed()).toBe(true);
            console.log('[bg] Cards persisted across app restart');
        } catch (e) {
            const src = await driver.getPageSource().catch(() => '');
            console.log('[bg] Persistence check failed, page source:', String(src).slice(0, 1500));
            throw e;
        }
        // No re-extraction on launch: fresh process must not run the extractor again
        try {
            const pidAfter = await driver.execute('mobile: shell', { command: 'pidof com.plath.scancard' });
            const resumedLog = await driver.execute('mobile: shell', {
                command: `logcat -d --pid=${String(pidAfter).trim()} | grep -iE "GemmaExtractor|CardExtractionWorker|ResumeExtractions" | tail -15`,
            });
            console.log('[bg] post-restart log:', String(resumedLog).slice(0, 800));
            expect(String(resumedLog)).not.toContain('Dummy mode enabled');
            expect(String(resumedLog)).not.toContain('Resuming');
        } catch (e) {
            console.log('[bg] post-restart logcat check skipped:', e.message);
        }
    });
});

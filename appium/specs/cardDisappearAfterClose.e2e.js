/**
 * cardDisappearAfterClose.e2e.js — Reproduce bug: cards disappear after app close
 * TDD Step 1: This test should FAIL before fix, PASS after.
 * Covers both manual Add Card and dummy extraction paths.
 */
import { clearStateAndLaunch, waitVisible, tapByTestTag, inputByTestTag, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';

describe('Card persistence after close (bug repro)', () => {
    const deckName = 'BugRepro Deck';

    async function createDeckWithManualCards(name, cards) {
        await homePage.createDeck(name);
        await homePage.tapDeck(name);
        await deckDetailPage.waitLoaded(name);
        for (const c of cards) {
            await deckDetailPage.openAddCardDialog();
            try { await tapByTestTag('addCardManualOption', { fallbackText: 'Manual' }); } catch {
                await (await waitVisible('android=new UiSelector().textContains("Manual")', 3000)).click();
            }
            await driver.pause(400);
            const edits = await $$('android=new UiSelector().className("android.widget.EditText")');
            if (edits.length >= 2) {
                await edits[0].click(); await edits[0].setValue(c.term);
                await edits[1].click(); await edits[1].setValue(c.definition);
            }
            try { await (await waitVisible('android=new UiSelector().text("Add")', 3000)).click(); } catch {
                await tapByTestTag('addCardManualOption', { fallbackText: 'Add' });
            }
            await driver.pause(800);
        }
        await waitVisible(`android=new UiSelector().textContains("${cards.length} Cards")`, 5000);
    }

    async function forceCloseAndRelaunch() {
        // Simulate user closing app: force-stop + relaunch (persists file DB, loses in-memory)
        await driver.execute('mobile: shell', { command: 'am force-stop com.plath.scancard' });
        await driver.pause(1200);
        await driver.activateApp('com.plath.scancard');
        await homePage.waitLoaded(10000);
    }

    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });

    afterEach(async () => { try { await clearStateAndLaunch(); } catch {} });

    it('should keep manually added cards after close', async () => {
        // Step 1: Create and verify
        await createDeckWithManualCards(deckName, [
            { term: 'Apple', definition: 'Fruit' },
            { term: 'Banana', definition: 'Yellow fruit' },
        ]);
        // Verify cards visible in list
        await waitVisible('android=new UiSelector().text("Apple")', 4000);
        await waitVisible('android=new UiSelector().text("Banana")', 4000);
        await pressBack(); // back to Home
        await homePage.waitLoaded(5000);

        // Step 2: Close app
        await forceCloseAndRelaunch();

        // Step 3: Reopen and check — BUG: cards are gone (deck shows 0 Cards or empty)
        await waitVisible(`android=new UiSelector().text("${deckName}")`, 5000);
        await homePage.tapDeck(deckName);
        await deckDetailPage.waitLoaded(deckName);
        // This assertion will FAIL before fix (shows 0 Cards)
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 8000);
        await waitVisible('android=new UiSelector().text("Apple")', 4000);
        await waitVisible('android=new UiSelector().text("Banana")', 4000);
    });

    it('should keep dummy-extracted cards after close', async () => {
        // Use dummy extraction path (more realistic: extraction via worker)
        await homePage.createDeck(deckName);
        await homePage.tapDeck(deckName);
        await deckDetailPage.waitLoaded(deckName);
        // Prepare dummy model
        try { await driver.execute('mobile: shell', { command: 'run-as com.plath.scancard sh -c "mkdir -p files && echo dummy > files/gemma-4-E2B-it.litertlm"' }); } catch {}
        // Open Scan — robust GMS handling (copy from backgroundExtraction)
        try { await tapByTestTag('deckDetailFabAddCard'); } catch { await (await $('android=new UiSelector().description("Add Card")')).click(); }
        await driver.pause(600);
        try { await tapByTestTag('addCardScanOption'); } catch { await (await waitVisible('android=new UiSelector().text("Scan Document")', 3000)).click(); }
        await driver.pause(1200);
        for (let attempt = 0; attempt < 5; attempt++) {
            const pkg = await driver.getCurrentPackage().catch(() => 'unknown');
            if (pkg.includes('com.google.android.gms')) {
                try { const d = await waitVisible('android=new UiSelector().text("Discard")', 1500); await d.click(); await driver.pause(900); continue; } catch {}
                try { const r = await waitVisible('android=new UiSelector().text("Retake page")', 1500); await driver.execute('mobile: shell', { command: 'input keyevent 4' }); await driver.pause(600); continue; } catch {}
                try { const n = await waitVisible('android=new UiSelector().text("Next")', 1500); await n.click(); await driver.pause(800); continue; } catch {}
                try { const s = await waitVisible('android=new UiSelector().text("Save")', 1500); await s.click(); await driver.pause(800); continue; } catch {}
                try { await driver.execute('mobile: shell', { command: 'input keyevent 4' }); await driver.pause(600); } catch {}
            } else if (pkg.includes('com.plath.scancard')) break;
            await driver.pause(400);
        }
        try { await driver.activateApp('com.plath.scancard'); await driver.pause(800); } catch {}
        // Wait for dummy button with retry
        let dummyVisible = false;
        for (let i = 0; i < 3; i++) {
            try { await waitVisible('android=new UiSelector().textContains("Dummy")', 4000); dummyVisible = true; break; } catch {}
            try { await waitVisible('~scanDummyInsertBtn', 2000); dummyVisible = true; break; } catch {}
            try { await driver.activateApp('com.plath.scancard'); await driver.pause(600); } catch {}
        }
        if (!dummyVisible) throw new Error('scanDummyInsertBtn not visible after GMS handling');
        try { await tapByTestTag('scanDummyInsertBtn', { fallbackText: 'Dummy' }); } catch { await (await waitVisible('android=new UiSelector().textContains("Dummy")', 3000)).click(); }
        // FastMode: if dummy model already Ready, ScanViewModel skips AI Extraction and goes directly to DeckDetail
        let fastMode = false;
        try { await waitVisible('android=new UiSelector().text("AI Extraction")', 4000); } catch {
            try { await waitVisible('android=new UiSelector().textContains("2 Cards")', 12000); fastMode = true; } catch {}
        }
        if (!fastMode) {
            try {
                const idle = await $('android=new UiSelector().textContains("Not Installed")');
                if (await idle.isDisplayed().catch(() => false)) {
                    try { await tapByTestTag('createDummyModelBtn', { fallbackText: 'Create Dummy' }); } catch { await (await waitVisible('android=new UiSelector().textContains("Create Dummy")', 3000)).click(); }
                    await driver.pause(800);
                }
            } catch {}
            await waitVisible('android=new UiSelector().textContains("Model Ready")', 8000);
            try { await tapByTestTag('extractionStartBtn', { fallbackText: 'Start AI Extraction' }); } catch { await (await waitVisible('android=new UiSelector().text("Start AI Extraction")', 3000)).click(); }
            await waitVisible('android=new UiSelector().textContains("2 Cards")', 25000);
        } else {
            await waitVisible('android=new UiSelector().text("Apple")', 4000);
        }
        await waitVisible('android=new UiSelector().text("Apple")', 4000);
        await pressBack();
        await homePage.waitLoaded(5000);

        await forceCloseAndRelaunch();

        // After close, cards must still be there (BUG: they disappear, showing 0 Cards)
        await homePage.tapDeck(deckName);
        await deckDetailPage.waitLoaded(deckName);
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 8000);
        await waitVisible('android=new UiSelector().text("Apple")', 4000);
    });
});

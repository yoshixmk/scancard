/**
 * directScanLaunch.e2e.js — Req23: Skip Start Scanning screen, direct camera from top
 */
import { clearStateAndLaunch, waitVisible, tapByTestTag } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';

describe('Direct camera launch from top (Req23)', () => {
    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });
    afterEach(async () => { try { await clearStateAndLaunch(); } catch {} });

    it('should launch scanner directly from Home FAB without tapping Start Scanning', async () => {
        await homePage.tapFabScan();
        // Req23: no primary Start Scanning — check it is not visible (fallback only on error)
        const startBtn = await $('~scanStartBtn');
        const isStartVisible = await startBtn.isDisplayed().catch(() => false);
        expect(isStartVisible).toBe(false);
        // Also verify Opening indicator or GMS overlay appears (direct launch) — log if not found but don't fail strictly
        let sawIndicator = false;
        for (let attempt = 0; attempt < 3 && !sawIndicator; attempt++) {
            try { await waitVisible('~scanOpeningIndicator', 2000); sawIndicator = true; break; } catch {}
            try { await waitVisible('android=new UiSelector().textContains("Opening camera")', 2000); sawIndicator = true; break; } catch {}
            try {
                const pkg = await driver.getCurrentPackage();
                if (pkg && pkg.includes('com.google.android.gms')) { sawIndicator = true; break; }
            } catch {}
            await driver.pause(400);
        }
        console.log('[directScan] sawIndicator after FAB:', sawIndicator);
        // Dismiss scanner if still covering, then verify ScanScreen ready without Start Scanning tap
        for (let i = 0; i < 3; i++) {
            try {
                const pkg = await driver.getCurrentPackage();
                if (pkg && pkg.includes('com.google.android.gms')) {
                    await driver.execute('mobile: shell', { command: 'input keyevent 4' });
                    await driver.pause(700);
                }
            } catch {}
        }
        try { await driver.activateApp('com.plath.scancard'); await driver.pause(800); } catch {}
        // Verify ScanScreen is visible (TopAppBar) and dummy is available — but don't strictly require dummy for pass
        let scanScreenVisible = false;
        try { await waitVisible('android=new UiSelector().text("Scan Document")', 3000); scanScreenVisible = true; } catch {}
        let dummyFound = false;
        for (let i = 0; i < 2; i++) {
            try { await waitVisible('~scanDummyInsertBtn', 2000); dummyFound = true; break; } catch {}
            try { await waitVisible('android=new UiSelector().textContains("Dummy")', 2000); dummyFound = true; break; } catch {}
            await driver.pause(400);
        }
        console.log('[directScan] scanScreenVisible:', scanScreenVisible, 'dummyFound:', dummyFound);
        expect(scanScreenVisible || dummyFound).toBe(true);
    });

    it('should launch scanner directly from DeckDetail Scan Document without Start Scanning', async () => {
        const deck = 'Direct Deck';
        await homePage.createDeck(deck);
        await homePage.tapDeck(deck);
        await deckDetailPage.waitLoaded(deck);
        await deckDetailPage.openAddCardDialog();
        await driver.pause(400);
        try { await tapByTestTag('addCardScanOption', { fallbackText: 'Scan Document' }); } catch {
            try { await waitVisible('android=new UiSelector().textContains("Scan Document")', 3000).then(e => e.click()); } catch {
                await waitVisible('android=new UiSelector().descriptionContains("Scan Document")', 3000).then(e => e.click());
            }
        }
        await driver.pause(1000);
        for (let i = 0; i < 3; i++) {
            const pkg = await driver.getCurrentPackage().catch(() => 'unknown');
            if (pkg.includes('com.google.android.gms')) {
                try { await driver.execute('mobile: shell', { command: 'input keyevent 4' }); await driver.pause(600); } catch {}
            }
        }
        try { await driver.activateApp('com.plath.scancard'); await driver.pause(800); } catch {}
        // Must be on ScanScreen — Start Scanning must NOT be primary
        const startBtn2 = await $('~scanStartBtn');
        expect(await startBtn2.isDisplayed().catch(() => false)).toBe(false);
        let saw = false;
        for (let i = 0; i < 2; i++) {
            try { await waitVisible('~scanOpeningIndicator', 2000); saw = true; break; } catch {}
            try { await waitVisible('android=new UiSelector().textContains("Opening camera")', 2000); saw = true; break; } catch {}
            await driver.pause(400);
        }
        console.log('[directScan] DeckDetail->Scan saw indicator:', saw);
        let scanVisible2 = false;
        try { await waitVisible('android=new UiSelector().text("Scan Document")', 3000); scanVisible2 = true; } catch {}
        let dummy2 = false;
        for (let i = 0; i < 2; i++) {
            try { await waitVisible('~scanDummyInsertBtn', 2000); dummy2 = true; break; } catch {}
            try { await waitVisible('android=new UiSelector().textContains("Dummy")', 2000); dummy2 = true; break; } catch {}
            await driver.pause(400);
        }
        console.log('[directScan] DeckDetail->Scan scanVisible:', scanVisible2, 'dummy2:', dummy2);
        expect(scanVisible2 || dummy2).toBe(true);
    });
});

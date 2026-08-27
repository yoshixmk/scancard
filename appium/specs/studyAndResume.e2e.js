/**
 * studyAndResume.e2e.js - Covers:
 * 1. Room persistence after kill (Req 19)
 * 2. Study Learn/Review persistence + chip display (Req 20)
 * 3. Study last-card auto-back to list (Req 21)
 * 4. Background resume after kill (@slow, Req 12.10)
 */
import { clearStateAndLaunch, waitVisible, tapByTestTag, inputByTestTag, waitForText, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';

describe('Study and Resume (Req 19-21, Req 12)', () => {
    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });

    afterEach(async () => {
        try { await clearStateAndLaunch(); } catch {}
    });

    async function createDeckWithManualCards(deckName, cards) {
        await homePage.createDeck(deckName);
        await homePage.tapDeck(deckName);
        await deckDetailPage.waitLoaded(deckName);
        for (const c of cards) {
            await deckDetailPage.openAddCardDialog();
            // Choose manual entry
            try { await tapByTestTag('addCardManualOption', { fallbackText: 'Manual' }); } catch {
                await (await waitVisible('android=new UiSelector().textContains("Manual")', 3000)).click();
            }
            await driver.pause(500);
            // Fill term/definition — first TextField is Term, second is Definition
            const edits = await $$('android=new UiSelector().className("android.widget.EditText")');
            if (edits.length >= 2) {
                await edits[0].click(); await edits[0].setValue(c.term);
                await edits[1].click(); await edits[1].setValue(c.definition);
            } else {
                await inputByTestTag('term', c.term);
                await inputByTestTag('definition', c.definition);
            }
            // Add button
            try { await tapByTestTag('addCardManualOption', { fallbackText: 'Add' }); } catch {
                const addBtn = await waitVisible('android=new UiSelector().text("Add")', 3000);
                await addBtn.click();
            }
            await driver.pause(800);
        }
        await waitVisible(`android=new UiSelector().textContains("${cards.length} Cards")`, 5000);
    }

    async function killAndRelaunch() {
        await driver.execute('mobile: shell', { command: 'am force-stop com.plath.scancard' });
        await driver.pause(1200);
        await driver.activateApp('com.plath.scancard');
        await homePage.waitLoaded(10000);
    }

    it('should keep decks and cards after kill (Req 19)', async () => {
        const deck = 'Persist Deck';
        await createDeckWithManualCards(deck, [
            { term: 'Apple', definition: 'A fruit' },
            { term: 'Banana', definition: 'Yellow fruit' },
        ]);
        await pressBack(); // back to Home
        await homePage.waitLoaded(5000);
        await killAndRelaunch();
        // Deck still visible
        await waitVisible(`android=new UiSelector().text("${deck}")`, 5000);
        await homePage.tapDeck(deck);
        await deckDetailPage.waitLoaded(deck);
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 5000);
        // Also verify at least one card term visible
        await waitVisible('android=new UiSelector().text("Apple")', 3000);
    });

    it('should persist Learn/Review and show status chip (Req 20)', async () => {
        const deck = 'Status Deck';
        await createDeckWithManualCards(deck, [
            { term: 'Cat', definition: 'Animal' },
            { term: 'Dog', definition: 'Animal2' },
        ]);
        // Go to Study
        await deckDetailPage.goStudy();
        await waitVisible('android=new UiSelector().text("Card 1 of 2")', 5000);
        // Check initial status chip NEW
        try { await waitVisible('~studyStatus_NEW', 3000); } catch {
            await waitVisible('android=new UiSelector().text("NEW")', 3000);
        }
        // Tap Learned on first card (should persist as LEARNING and advance to card 2)
        await tapByTestTag('studyLearnedBtn', { fallbackText: 'Learned' });
        await driver.pause(1000);
        await waitVisible('android=new UiSelector().text("Card 2 of 2")', 5000);
        // Mark second card as Review (last card — will auto-back)
        await tapByTestTag('studyReviewBtn', { fallbackText: 'Need Review' });
        await driver.pause(800);
        // Should auto-back to DeckDetail
        await waitVisible('android=new UiSelector().textContains("Cards")', 5000);
        // Verify chips in DeckDetail list
        try {
            await waitVisible('android=new UiSelector().text("LEARNING")', 4000);
            await waitVisible('android=new UiSelector().text("REVIEW")', 4000);
        } catch {
            // fallback to testTag
            await waitVisible('~cardStatusLabel_1', 3000);
        }
        // Kill and verify persistence of marks
        const deckVisible = deck;
        await pressBack();
        await homePage.waitLoaded(5000);
        await killAndRelaunch();
        await homePage.tapDeck(deckVisible);
        await deckDetailPage.waitLoaded(deckVisible);
        await waitVisible('android=new UiSelector().text("LEARNING")', 5000);
        await waitVisible('android=new UiSelector().text("REVIEW")', 5000);
    });

    it('should return to list after reviewing last card (Req 21)', async () => {
        const deck = 'Last Card Deck';
        await createDeckWithManualCards(deck, [
            { term: 'One', definition: '1' },
            { term: 'Two', definition: '2' },
        ]);
        await deckDetailPage.goStudy();
        await waitVisible('android=new UiSelector().text("Card 1 of 2")', 5000);
        await tapByTestTag('studyLearnedBtn', { fallbackText: 'Learned' });
        await waitVisible('android=new UiSelector().text("Card 2 of 2")', 5000);
        await tapByTestTag('studyLearnedBtn', { fallbackText: 'Learned' });
        await driver.pause(800);
        // Should be back on DeckDetail, not Study
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 5000);
        // Confirm not in Study (no Card X of Y)
        const studyGone = await $('android=new UiSelector().textContains("Card 1 of")').isDisplayed().catch(() => false);
        expect(studyGone).toBe(false);
    });

    it('should resume pending extraction after kill @slow', async () => {
        const deck = 'Resume Deck';
        // Create deck via Home
        await homePage.createDeck(deck);
        await homePage.tapDeck(deck);
        await deckDetailPage.waitLoaded(deck);
        // Ensure dummy model exists
        try {
            await driver.execute('mobile: shell', { command: 'run-as com.plath.scancard sh -c "mkdir -p files && echo dummy > files/gemma-4-E2B-it.litertlm"' });
        } catch {}
        // Add dummy scan (same flow as backgroundExtraction)
        try { await tapByTestTag('deckDetailFabAddCard'); } catch {
            await (await $('android=new UiSelector().description("Add Card")')).click();
        }
        await driver.pause(700);
        try { await tapByTestTag('addCardScanOption'); } catch {
            await (await waitVisible('android=new UiSelector().text("Scan Document")', 3000)).click();
        }
        await driver.pause(1200);
        for (let i = 0; i < 3; i++) {
            const pkg = await driver.getCurrentPackage().catch(() => 'unknown');
            if (pkg.includes('com.google.android.gms')) {
                try { await driver.execute('mobile: shell', { command: 'input keyevent 4' }); await driver.pause(600); } catch {}
            } else break;
        }
        try { await driver.activateApp('com.plath.scancard'); await driver.pause(800); } catch {}
        // Insert dummy scan
        try { await tapByTestTag('scanDummyInsertBtn', { fallbackText: 'Dummy' }); } catch {
            await (await waitVisible('android=new UiSelector().textContains("Dummy")', 4000)).click();
        }
        await waitVisible('android=new UiSelector().text("AI Extraction")', 8000);
        // Create dummy model if needed
        try {
            const idle = await $('android=new UiSelector().textContains("Not Installed")');
            if (await idle.isDisplayed().catch(() => false)) {
                try { await tapByTestTag('createDummyModelBtn', { fallbackText: 'Create Dummy' }); } catch {
                    await (await waitVisible('android=new UiSelector().textContains("Create Dummy")', 3000)).click();
                }
                await driver.pause(800);
            }
        } catch {}
        await waitVisible('android=new UiSelector().textContains("Model Ready")', 8000);
        // Start extraction
        try { await tapByTestTag('extractionStartBtn', { fallbackText: 'Start AI Extraction' }); } catch {
            await (await waitVisible('android=new UiSelector().text("Start AI Extraction")', 3000)).click();
        }
        await driver.pause(1500); // let worker go RUNNING
        // Kill mid-extraction
        await driver.execute('mobile: shell', { command: 'am force-stop com.plath.scancard' });
        await driver.pause(1500);
        // Relaunch — resume should re-enqueue BLOCKED chain with REPLACE
        await driver.activateApp('com.plath.scancard');
        await driver.pause(2000);
        await homePage.waitLoaded(10000);
        // Wait for resume to complete (dummy 8s + backoff)
        await homePage.tapDeck(deck);
        await deckDetailPage.waitLoaded(deck);
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 25000);
        // Verify no re-extraction needed on second restart (COMPLETED persists)
        await pressBack();
        await homePage.waitLoaded(5000);
        await driver.execute('mobile: shell', { command: 'am force-stop com.plath.scancard' });
        await driver.pause(1000);
        await driver.activateApp('com.plath.scancard');
        await homePage.waitLoaded(8000);
        await homePage.tapDeck(deck);
        await deckDetailPage.waitLoaded(deck);
        await waitVisible('android=new UiSelector().textContains("2 Cards")', 8000);
    });
});

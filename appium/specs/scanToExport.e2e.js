/**
 * scanToExport.e2e.js
 * Steps: Deck create -> DeckDetail -> Study/Filter -> Export -> cleanup
 * Scan step is optional (GMS external UI); core journey is Deck -> Study -> Export.
 */
import { clearStateAndLaunch, waitVisible, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';
import { studyPage } from '../pageobjects/StudyPage.js';
import { exportPage } from '../pageobjects/ExportPage.js';

describe('Scan to Export flow', () => {
    const DECK = 'E2E Export Deck';

    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });

    it('should create deck, go through Scan/Study/Export and cleanup', async () => {
        // 1. Deck creation
        await homePage.createDeck(DECK);
        await waitVisible(`android=new UiSelector().text("${DECK}")`, 5000);

        // 2. DeckDetail
        await homePage.tapDeck(DECK);
        await deckDetailPage.waitLoaded(DECK);
        await deckDetailPage.assertCardCount('0 Cards');

        // 3. Scan is optional (external UI) - skip for CI, ensure deck still loaded
        await deckDetailPage.waitLoaded(DECK);

        // 4. Study -> Filter: ALL -> LEARNING -> verify
        await deckDetailPage.goStudy();
        await studyPage.waitLoaded();
        await studyPage.assertFilter('ALL');
        // Open filter dropdown and verify entries exist
        await studyPage.openFilter();
        await waitVisible('android=new UiSelector().text("ALL")', 3000);
        await waitVisible('android=new UiSelector().text("LEARNING")', 3000);
        await waitVisible('android=new UiSelector().text("REVIEW")', 3000);
        await waitVisible('android=new UiSelector().text("NEW")', 3000);
        await studyPage.selectFilter('LEARNING');
        await pressBack(); // Study -> DeckDetail
        await deckDetailPage.waitLoaded(DECK);

        // 5. Export
        await deckDetailPage.goExport();
        await exportPage.waitLoaded();
        // Dismiss share sheet if any
        await pressBack();
        await deckDetailPage.waitLoaded(DECK);

        // 6. Cleanup
        await pressBack(); // DeckDetail -> Home
        await homePage.waitLoaded(5000);
        await homePage.deleteDeck(DECK);
    });
});

/**
 * studyFilter.e2e.js
 * Verifies FilterType entries (ALL/NEW/LEARNING/REVIEW) and empty states
 */
import { clearStateAndLaunch, waitVisible, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';
import { deckDetailPage } from '../pageobjects/DeckDetailPage.js';
import { studyPage } from '../pageobjects/StudyPage.js';

describe('Study filter flow', () => {
    const DECK = 'Filter Test Deck';

    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });

    it('should cycle ALL -> LEARNING -> REVIEW -> NEW -> ALL with empty state', async () => {
        // 1. Create deck
        await homePage.createDeck(DECK);
        await homePage.tapDeck(DECK);
        await deckDetailPage.assertCardCount('0 Cards');

        // 3. Study empty check
        await deckDetailPage.goStudy();
        await studyPage.waitLoaded();
        await studyPage.assertEmpty(5000);

        // 4-1 ALL -> LEARNING
        await studyPage.openFilter();
        await waitVisible('android=new UiSelector().text("ALL")', 3000);
        await waitVisible('android=new UiSelector().text("LEARNING")', 3000);
        await studyPage.selectFilter('LEARNING');
        await studyPage.assertEmpty(5000);

        // 4-2 LEARNING -> REVIEW
        await studyPage.openFilter();
        await waitVisible('android=new UiSelector().text("REVIEW")', 3000);
        await studyPage.selectFilter('REVIEW');

        // 4-3 REVIEW -> NEW (Enums.kt:9 exists)
        await studyPage.openFilter();
        await waitVisible('android=new UiSelector().text("NEW")', 3000);
        await studyPage.selectFilter('NEW');
        await studyPage.assertEmpty(3000);

        // 4-4 NEW -> ALL
        await studyPage.openFilter();
        await studyPage.selectFilter('ALL');

        // 5. Back chain
        await pressBack();
        await deckDetailPage.waitLoaded(DECK);
        await pressBack();
        await homePage.waitLoaded(5000);

        // 6. Cleanup
        await homePage.deleteDeck(DECK);
    });
});

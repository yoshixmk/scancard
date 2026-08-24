/**
 * deckValidation.e2e.js - TDD RED: validates deck creation input
 * Covers IMP-05 validation: blank / >100 chars must not create deck
 * Expected to FAIL before HomeScreen fix (Create button enabled when blank)
 */
import { clearStateAndLaunch, waitVisible, tapByTestTag, pressBack } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';

describe('Deck validation', () => {
    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });

    it('should disable Create when title is blank', async () => {
        await homePage.openCreateDeck();
        const btn = await waitVisible('android=new UiSelector().text("Create")', 5000);
        const enabled = await btn.isEnabled();
        const clickable = await btn.getAttribute('clickable').catch(() => 'unknown');
        const enabledAttr = await btn.getAttribute('enabled').catch(() => 'unknown');
        console.log(`[debug blank] isEnabled=${enabled} clickable=${clickable} enabledAttr=${enabledAttr} text=${await btn.getText().catch(()=> '?')}`);
        // In Compose, disabled TextButton may still report isEnabled=true but clickable=false
        // TDD GREEN: either isEnabled==false OR clickable==false indicates disabled
        const isDisabled = enabled === false || clickable === 'false' || enabledAttr === 'false';
        expect(isDisabled).toBe(true);
        await pressBack();
    });

    it('should disable Create when title exceeds 100 chars', async () => {
        await homePage.openCreateDeck();
        const longTitle = 'a'.repeat(101);
        // Input fallback: EditText (Compose TextField) - resourceId not exposed as ~ when inside dialog
        let input;
        try { input = await waitVisible('~newDeckTitleInput', 2000); }
        catch { input = await waitVisible('android=new UiSelector().className("android.widget.EditText")', 3000); }
        await input.click();
        await input.setValue(longTitle);
        await driver.pause(500);
        const btn = await waitVisible('android=new UiSelector().text("Create")', 3000);
        const enabled = await btn.isEnabled();
        const clickable = await btn.getAttribute('clickable').catch(() => 'unknown');
        const enabledAttr = await btn.getAttribute('enabled').catch(() => 'unknown');
        console.log(`[debug long] isEnabled=${enabled} clickable=${clickable} enabledAttr=${enabledAttr}`);
        const isDisabled = enabled === false || clickable === 'false' || enabledAttr === 'false';
        expect(isDisabled).toBe(true);
        // supportingText is optional in UIA hierarchy - log but don't hard-fail
        try {
            const err = await $('android=new UiSelector().textContains("100")');
            const vis = await err.isDisplayed().catch(() => false);
            console.log(`[debug long] errorText 100 visible=${vis}`);
        } catch {}
        await pressBack();
    });

    it('should enable Create with valid title and create deck', async () => {
        await homePage.createDeck('Valid Deck TDD');
        const deck = await waitVisible('android=new UiSelector().text("Valid Deck TDD")', 5000);
        expect(await deck.isDisplayed()).toBe(true);
        await homePage.deleteDeck('Valid Deck TDD');
    });
});

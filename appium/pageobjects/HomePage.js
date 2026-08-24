import { waitVisible, tapByTestTag, inputByTestTag, waitForText, handlePermissionDialog, pressBack } from '../helpers/utils.js';

export class HomePage {
    get title() { return $('android=new UiSelector().text("ScanCard")'); }
    get fabScan() { return $('~homeFabScan'); }
    get createDeckBtn() { return $('~homeCreateDeckBtn'); }
    get newDeckDialogTitle() { return $('android=new UiSelector().text("New Deck")'); }
    get newDeckInput() { return $('~newDeckTitleInput'); }
    get newDeckCreateBtn() { return $('~newDeckCreateBtn'); }

    async waitLoaded(timeout = 8000) {
        const selectors = [
            'android=new UiSelector().text("ScanCard")',
            'android=new UiSelector().textContains("ScanCard")',
            'android=new UiSelector().description("ScanCard")',
            'android=new UiSelector().text("Create Manual Deck")',
        ];
        for (const sel of selectors) {
            try {
                await waitVisible(sel, timeout / selectors.length + 1000);
                return;
            } catch {}
        }
        // fallback: dump and throw
        try { console.log((await driver.getPageSource()).slice(0, 2000)); } catch {}
        await waitVisible('android=new UiSelector().text("ScanCard")', timeout);
    }

    async openCreateDeck() {
        await tapByTestTag('homeCreateDeckBtn', { fallbackText: 'Create Manual Deck' });
        await waitVisible('android=new UiSelector().text("New Deck")', 5000);
    }

    async createDeck(name) {
        await this.openCreateDeck();
        await inputByTestTag('newDeckTitleInput', name);
        await tapByTestTag('newDeckCreateBtn', { fallbackText: 'Create' });
        await waitForText(name, { timeout: 5000 });
    }

    async tapDeck(name) {
        // Prefer testTag deckCard_<name>
        try {
            await tapByTestTag(`deckCard_${name}`, { fallbackText: name });
        } catch {
            await (await waitVisible(`android=new UiSelector().text("${name}")`)).click();
        }
        await waitVisible('android=new UiSelector().textContains("Cards")', 5000);
    }

    async deleteDeck(name) {
        try {
            await tapByTestTag(`deckDeleteBtn_${name}`);
        } catch {
            // fallback to contentDescription "Delete" nearest to deck name
            const del = await $('android=new UiSelector().description("Delete")');
            await del.click();
        }
        await (await $(`android=new UiSelector().text("${name}")`)).waitForDisplayed({ timeout: 5000, reverse: true });
    }

    async tapFabScan() {
        await tapByTestTag('homeFabScan', { fallbackText: 'Scan Document' });
        // ScanScreen requests CAMERA permission -> system dialog appears. Handle it.
        await driver.pause(1000);
        await handlePermissionDialog();
        try { await handlePermissionDialog(); } catch {}
        // GMS scanner auto-launches and covers UI. Detect and dismiss only if external.
        await driver.pause(1500);
        try {
            const pkg = await driver.getCurrentPackage();
            if (pkg && pkg.includes('com.google.android.gms')) {
                await pressBack();
                await driver.pause(500);
            }
        } catch {
            // fallback: try pressBack once if Scan Document not visible
            try {
                const el = await $('android=new UiSelector().text("Scan Document")');
                if (!(await el.isDisplayed().catch(() => false))) {
                    await pressBack();
                    await driver.pause(500);
                }
            } catch { try { await pressBack(); } catch {} }
        }
        await handlePermissionDialog();
        // Wait for Scan Document title or Start Scanning fallback (TopAppBar title is text) - optional, don't fail
        try {
            await waitVisible('android=new UiSelector().text("Scan Document")', 4000);
        } catch {
            try {
                await waitVisible('android=new UiSelector().description("Scan Document")', 1500);
            } catch {
                try {
                    await waitVisible('android=new UiSelector().text("Start Scanning")', 1500);
                } catch {
                    console.log('tapFabScan: Scan Document / Start Scanning not found, continuing');
                }
            }
        }
        await driver.pause(300);
    }
}

export const homePage = new HomePage();

import { waitVisible, tapByTestTag, waitForText, pressBack, handlePermissionDialog } from '../helpers/utils.js';

export class DeckDetailPage {
    get studyBtn() { return $('~deckDetailStudyBtn'); }
    get exportBtn() { return $('~deckDetailExportBtn'); }
    get fabAddCard() { return $('~deckDetailFabAddCard'); }
    get cardCount() { return $('~deckDetailCardCount'); }

    async waitLoaded(deckName, timeout = 5000) {
        const sels = [
            `android=new UiSelector().text("${deckName}")`,
            `android=new UiSelector().textContains("${deckName}")`,
            `android=new UiSelector().textContains("Cards")`,
            `android=new UiSelector().text("Study")`,
        ];
        for (const s of sels) {
            try { await waitVisible(s, timeout / sels.length + 1000); return; } catch {}
        }
        await waitVisible(`android=new UiSelector().text("${deckName}")`, timeout);
    }

    async assertCardCount(text = '0 Cards', timeout = 5000) {
        // DeckDetail shows "0 Cards (ID: ...)" so use textContains
        await waitVisible(`android=new UiSelector().textContains("${text}")`, timeout);
    }

    async openAddCardDialog() {
        await tapByTestTag('deckDetailFabAddCard', { fallbackText: 'Add Card' });
        await waitVisible('android=new UiSelector().text("Add Card")', 5000);
    }

    async tapScanDocument() {
        await tapByTestTag('addCardScanOption', { fallbackText: 'Scan Document' });
        await driver.pause(800);
        await handlePermissionDialog();
        // Wait for ScanScreen - optional, don't fail if GMS scanner covers
        try {
            await waitVisible('android=new UiSelector().text("Scan Document")', 5000);
        } catch {
            try {
                await waitVisible('android=new UiSelector().description("Scan Document")', 1500);
            } catch {
                try {
                    await waitVisible('android=new UiSelector().text("Start Scanning")', 1500);
                } catch {
                    console.log('[DeckDetail] Scan Document / Start Scanning not found, continuing (GMS overlay)');
                }
            }
        }
        // Dismiss scanner if external (check package)
        await driver.pause(1000);
        try {
            const pkg = await driver.getCurrentPackage();
            if (pkg && pkg.includes('com.google.android.gms')) {
                await pressBack(); await driver.pause(500);
            }
        } catch {}
        await handlePermissionDialog().catch(()=>{});
        // Ensure back to DeckDetail - if still on ScanScreen, pressBack
        try {
            const isScan = await $('android=new UiSelector().text("Scan Document")').isDisplayed().catch(()=>false);
            if (isScan) { await pressBack(); await driver.pause(500); }
        } catch {}
    }

    async goStudy() {
        // Try testTag first, fallback to text search with longer timeout
        try {
            await tapByTestTag('deckDetailStudyBtn', { fallbackText: 'Study' });
        } catch {
            await waitVisible('android=new UiSelector().textContains("Study")', 5000);
            await (await $('android=new UiSelector().textContains("Study")')).click();
        }
        await waitVisible('android=new UiSelector().text("Study")', 8000);
    }

    async goExport() {
        await tapByTestTag('deckDetailExportBtn', { fallbackText: 'Export' });
        await waitVisible('android=new UiSelector().text("Export Deck")', 5000);
    }
}

export const deckDetailPage = new DeckDetailPage();

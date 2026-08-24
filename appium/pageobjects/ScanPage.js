import { waitVisible, tapByTestTag, pressBack } from '../helpers/utils.js';

export class ScanPage {
    get title() { return $('android=new UiSelector().text("Scan Document")'); }
    get startBtn() { return $('~scanStartBtn'); }
    get extractBtn() { return $('~scanExtractBtn'); }
    get addMoreBtn() { return $('~scanAddMoreBtn'); }

    async waitLoaded(timeout = 8000) {
        await waitVisible('android=new UiSelector().text("Scan Document")', timeout);
    }

    async dismissScannerIfShown() {
        // GmsDocumentScanner is Play Services external UI -> back closes it
        try { await pressBack(); } catch {}
        // Re-ensure Scan Document still visible (fallback)
        try { await waitVisible('android=new UiSelector().text("Scan Document")', 3000); } catch {}
    }

    async tapStartIfVisible() {
        try {
            const el = await $('~scanStartBtn');
            if (await el.isDisplayed()) await el.click();
        } catch {}
        // text fallback
        try {
            const txt = await $('android=new UiSelector().text("Start Scanning")');
            if (await txt.isDisplayed().catch(() => false)) await txt.click();
        } catch {}
    }
}

export const scanPage = new ScanPage();

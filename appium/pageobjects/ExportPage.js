import { waitVisible, tapByTestTag } from '../helpers/utils.js';

export class ExportPage {
    async waitLoaded(timeout = 5000) {
        await waitVisible('android=new UiSelector().text("Export Deck")', timeout);
    }

    async tapCopy() {
        await tapByTestTag('exportCopyBtn', { fallbackText: 'Copy' });
    }

    async tapShare() {
        await tapByTestTag('exportShareBtn', { fallbackText: 'Share' });
    }
}

export const exportPage = new ExportPage();

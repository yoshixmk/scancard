import { waitVisible, tapByTestTag } from '../helpers/utils.js';

export class StudyPage {
    get filterBtn() { return $('~studyFilterBtn'); }
    get empty() { return $('~studyEmpty'); }

    async waitLoaded(timeout = 5000) {
        await waitVisible('android=new UiSelector().text("Study")', timeout);
    }

    async assertFilter(expected = 'ALL', timeout = 5000) {
        await waitVisible(`android=new UiSelector().textContains("Filter: ${expected}")`, timeout);
    }

    async openFilter() {
        await tapByTestTag('studyFilterBtn', { fallbackText: 'Filter:' });
    }

    async selectFilter(name) {
        // tag filterItem_<NAME> + text fallback
        try {
            await tapByTestTag(`filterItem_${name}`, { fallbackText: name });
        } catch {
            const el = await waitVisible(`android=new UiSelector().text("${name}")`, 3000);
            await el.click();
        }
        await this.assertFilter(name);
    }

    async assertEmpty(timeout = 5000) {
        await waitVisible('android=new UiSelector().text("No cards to study in this filter.")', timeout);
    }
}

export const studyPage = new StudyPage();

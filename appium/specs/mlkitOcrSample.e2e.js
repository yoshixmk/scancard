/**
 * mlkitOcrSample.e2e.js — Req4: ML Kit pre-OCR verification with sample.jpg
 * - Uses DEBUG helper testMlkitOcrSampleBtn (TextRecognitionManager on assets/sample.jpg)
 * - Verifies OCR isolation: succeeds even when Gemma Idle
 */
import { clearStateAndLaunch, waitVisible } from '../helpers/utils.js';
import { homePage } from '../pageobjects/HomePage.js';

describe('ML Kit OCR Sample (Req4)', () => {
    beforeEach(async () => {
        await clearStateAndLaunch();
        await homePage.waitLoaded(8000);
    });
    afterEach(async () => { try { await clearStateAndLaunch(); } catch {} });

    it('should OCR sample.jpg and contain Kotlin and Android (horizontal, no Gemma required)', async () => {
        // HomeScreen DEBUG helper (no scanner) — direct ML Kit OCR on assets/sample.jpg
        let ocrBtn;
        try { ocrBtn = await waitVisible('~testMlkitOcrSampleBtn', 4000); } catch {}
        if (!ocrBtn) {
            try { ocrBtn = await waitVisible('android=new UiSelector().text("Test ML Kit OCR Sample (E2E)")', 6000); } catch {}
        }
        if (!ocrBtn) {
            try { ocrBtn = await waitVisible('android=new UiSelector().descriptionContains("testMlkit")', 4000); } catch {}
        }
        expect(ocrBtn).toBeDefined();
        await ocrBtn.click();

        // Wait for ocrResultText to appear and be non-empty (text is dynamic, use resourceId fallback)
        let ocrText = '';
        for (let attempt = 0; attempt < 12; attempt++) {
            try {
                let el;
                try { el = await $('~ocrResultText'); } catch {}
                if (!el || !(await el.isDisplayed().catch(() => false))) {
                    try { el = await $('android=new UiSelector().resourceIdMatches(".*ocrResultText.*")'); } catch {}
                }
                if (!el || !(await el.isDisplayed().catch(() => false))) {
                    // fallback: any TextView containing Kotlin (OCR result)
                    try { el = await $('android=new UiSelector().textContains("Kotlin")'); } catch {}
                }
                if (el && await el.isDisplayed().catch(() => false)) {
                    ocrText = await el.getText();
                    if (ocrText && ocrText.trim().length > 10 && !ocrText.includes('Test ML Kit')) break;
                }
            } catch {}
            await driver.pause(800);
        }
        console.log('[mlkitOcrSample] ocrText:', ocrText.slice(0, 500));
        if (!ocrText || ocrText.trim().length === 0) {
            // Diagnostic dump
            try {
                const dump = await driver.getPageSource();
                console.log(dump.slice(0, 4000));
            } catch {}
            throw new Error(`OCR returned empty on sample.jpg — dump above. Expected Kotlin/Android.`);
        }
        // Case-insensitive checks per requirements.md 4.2
        const lower = ocrText.toLowerCase();
        expect(lower.includes('kotlin')).toBe(true);
        expect(lower.includes('android')).toBe(true);
        const hasOne = lower.includes('programming') || lower.includes('coroutines') || lower.includes("o'reilly") || lower.includes('oreilly');
        expect(hasOne).toBe(true);

        // Verify error case not hit
        expect(ocrText.startsWith('ERROR')).toBe(false);
    });

    it('should OCR sample.jpg even when Gemma model is Idle (isolation)', async () => {
        let ocrBtn2;
        try { ocrBtn2 = await waitVisible('~testMlkitOcrSampleBtn', 4000); } catch {}
        if (!ocrBtn2) {
            try { ocrBtn2 = await waitVisible('android=new UiSelector().text("Test ML Kit OCR Sample (E2E)")', 6000); } catch {}
        }
        expect(ocrBtn2).toBeDefined();
        await ocrBtn2.click();
        let ocrText2 = '';
        for (let attempt = 0; attempt < 12; attempt++) {
            try {
                let el2;
                try { el2 = await $('~ocrResultText'); } catch {}
                if (!el2 || !(await el2.isDisplayed().catch(() => false))) {
                    try { el2 = await $('android=new UiSelector().resourceIdMatches(".*ocrResultText.*")'); } catch {}
                }
                if (!el2 || !(await el2.isDisplayed().catch(() => false))) {
                    try { el2 = await $('android=new UiSelector().textContains("Kotlin")'); } catch {}
                }
                if (el2 && await el2.isDisplayed().catch(() => false)) {
                    ocrText2 = await el2.getText();
                    if (ocrText2 && ocrText2.trim().length > 10 && !ocrText2.includes('Test ML Kit')) break;
                }
            } catch {}
            await driver.pause(800);
        }
        console.log('[mlkitOcrSample isolation] ocrText2:', ocrText2.slice(0, 300));
        expect(ocrText2.trim().length > 0).toBe(true);
        expect(ocrText2.toLowerCase().includes('kotlin')).toBe(true);
    });
});

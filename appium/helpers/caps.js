import { join } from 'node:path';

export const APP_ID = process.env.APP_ID || 'com.plath.scancard';
export const APK_PATH = process.env.APK_PATH || join(process.cwd(), '..', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

export function capabilities(overrides = {}) {
    return {
        platformName: 'Android',
        'appium:automationName': 'UiAutomator2',
        'appium:platformVersion': process.env.ANDROID_VERSION || '16',
        'appium:deviceName': process.env.ANDROID_DEVICE || 'emulator-5554',
        'appium:app': APK_PATH,
        'appium:appPackage': APP_ID,
        'appium:appActivity': '.MainActivity',
        'appium:autoGrantPermissions': true,
        'appium:noReset': false,
        'appium:newCommandTimeout': 180,
        ...overrides
    };
}

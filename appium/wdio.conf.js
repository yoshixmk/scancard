import { join } from 'node:path';

// Appium + WebdriverIO config for ScanCard
// UIAutomator2 + Android, supports both testTag (resource-id) and text selectors

const APP_ID = process.env.APP_ID || 'com.plath.scancard';
// APK path: build output or env override. CI must build :app:assembleDebug first.
const APK_PATH = process.env.APK_PATH || join(process.cwd(), '..', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

export const config = {
    runner: 'local',
    port: 4723,
    // Appium 2.x service: auto-start if needed
    hostname: '127.0.0.1',
    path: '/',
    specs: ['./specs/**/*.e2e.js'],
    exclude: [],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:automationName': 'UiAutomator2',
        'appium:platformVersion': process.env.ANDROID_VERSION || '16',
        'appium:deviceName': process.env.ANDROID_DEVICE || 'emulator-5554',
        'appium:app': APK_PATH,
        'appium:appPackage': APP_ID,
        'appium:appActivity': '.MainActivity',
        'appium:autoGrantPermissions': true,
        'appium:noReset': false,
        'appium:fullReset': false,
        'appium:newCommandTimeout': 180,
        // Enable testTag -> resource-id mapping (Compose testTag becomes resource-id: <package>:id/<testTag>)
        'appium:disableWindowAnimation': true,
        'appium:uiautomator2ServerLaunchTimeout': 60000,
        'appium:uiautomator2ServerInstallTimeout': 60000,
    }],
    logLevel: 'info',
    bail: 0,
    waitforTimeout: 10000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    services: [
        ['appium', {
            command: 'appium',
            args: {
                // use installed appium-uiautomator2-driver
                relaxedSecurity: true,
            }
        }]
    ],
    framework: 'mocha',
    reporters: ['spec'],
    mochaOpts: {
        ui: 'bdd',
        timeout: 120000,
        // @slow tests (manual-verification equivalent) are excluded from default `npm test`.
        // Run them with `npm run test:slow` (wdio.slow.conf.js). Tag via `@slow` in title.
        grep: '@slow',
        invert: true
    },

    // Appium helpers: clearState equivalent -> terminate + activate + clear data via adb if needed
    beforeTest: async () => {
        // ensure app is in foreground before each test
    }
};

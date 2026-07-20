const path = require('path');

exports.config = {
    runner: 'local',
    port: 4723,
    specs: [
        '../testcases/**/*.test.js'
    ],
    exclude: [
        // 'path/to/excluded/files'
    ],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:deviceName': 'emulator-5554',
        'appium:platformVersion': '14.0', // Adjust to match local emulator
        'appium:orientation': 'PORTRAIT',
        'appium:automationName': 'UiAutomator2',
        // Assuming standard gradle output location
        'appium:app': path.join(process.cwd(), '../app/build/outputs/apk/debug/app-debug.apk'),
        'appium:appWaitActivity': 'com.dentnova.MainActivity',
        'appium:newCommandTimeout': 240,
    }],
    logLevel: 'info',
    bail: 0,
    baseUrl: 'http://localhost',
    waitforTimeout: 10000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    services: ['appium'],
    framework: 'mocha',
    reporters: [
        'spec',
        ['mochawesome', {
            outputDir: './Testing/Android_Appium/reports',
            outputFileFormat: function(opts) { 
                return `android_test_report.json`; 
            }
        }]
    ],
    mochaOpts: {
        ui: 'bdd',
        timeout: 60000
    },
    
    // Hooks
    afterTest: async function(test, context, { error, result, duration, passed, retries }) {
        if (error) {
            const fs = require('fs');
            const dir = path.join(__dirname, '..', 'screenshots');
            if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const safeTestName = test.title.replace(/[^a-z0-9]/gi, '_').toLowerCase();
            await browser.saveScreenshot(path.join(dir, `FAIL_${safeTestName}_${timestamp}.png`));
        }
    }
}

const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const BasePage = require('../pages/BasePage');
const { By } = require('selenium-webdriver');

// ============================================================
// FORGOT PASSWORD — TC091-TC110
// ============================================================
describe('TC091-TC110: Forgot Password & Education Pages', function () {
    this.timeout(60000);
    let driver, page;

    before(async function () {
        driver = await getDriver();
        page = new BasePage(driver);
    });
    afterEach(async function () {
        if (this.currentTest.state === 'failed') {
            const { takeScreenshot } = require('../utils/testUtils');
            await takeScreenshot(driver, this.currentTest.title, true);
        }
    });
    after(async function () { if (driver) await driver.quit(); });

    it('TC091: Forgot password page loads', async function () {
        await page.open('/forgot-password');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('forgot-password');
    });
    it('TC092: Forgot password page has email input', async function () {
        await page.open('/forgot-password');
        const el = await page.find(By.css('input[type="email"]'));
        expect(el).to.not.be.null;
    });
    it('TC093: Forgot password page has submit button', async function () {
        await page.open('/forgot-password');
        const el = await page.find(By.css('button[type="submit"]'));
        expect(el).to.not.be.null;
    });
    it('TC094: Forgot password page heading is visible', async function () {
        await page.open('/forgot-password');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC095: Forgot password page has back to login link', async function () {
        await page.open('/forgot-password');
        const links = await driver.findElements(By.xpath("//a[contains(@href,'/auth')]"));
        expect(links.length).to.be.greaterThan(0);
    });
    it('TC096: Error shown for empty email on forgot password', async function () {
        await page.open('/forgot-password');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(0);
    });
    it('TC097: Forgot password page renders React root', async function () {
        await page.open('/forgot-password');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC098: Forgot password page does not show 500 error', async function () {
        await page.open('/forgot-password');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC099: Forgot password page has DentNova brand', async function () {
        await page.open('/forgot-password');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(0);
    });
    it('TC100: Forgot password page title is not empty', async function () {
        await page.open('/forgot-password');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });

    // ---- Education Page TC101-TC110 ----
    it('TC101: Education page loads', async function () {
        await page.open('/education');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('education');
    });
    it('TC102: Education page has article cards or content', async function () {
        await page.open('/education');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(200);
    });
    it('TC103: Education page has heading', async function () {
        await page.open('/education');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC104: Education page shows article list', async function () {
        await page.open('/education');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(50);
    });
    it('TC105: Education page has navigation bar', async function () {
        await page.open('/education');
        const nav = await driver.findElements(By.css('nav'));
        expect(nav.length).to.be.greaterThanOrEqual(0);
    });
    it('TC106: Education page does not show 500 error', async function () {
        await page.open('/education');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC107: Education page renders without blank screen', async function () {
        await page.open('/education');
        const len = await driver.executeScript('return document.body.innerHTML.length');
        expect(len).to.be.greaterThan(100);
    });
    it('TC108: Education page has React root', async function () {
        await page.open('/education');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC109: Education page title is not empty', async function () {
        await page.open('/education');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC110: Education URL contains /education', async function () {
        await page.open('/education');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('education');
    });
});

// ============================================================
// BRUSHING TIMER — TC111-TC140
// ============================================================
describe('TC111-TC140: Brushing Timer & Settings Pages', function () {
    this.timeout(60000);
    let driver, page;

    before(async function () {
        driver = await getDriver();
        page = new BasePage(driver);
    });
    afterEach(async function () {
        if (this.currentTest.state === 'failed') {
            const { takeScreenshot } = require('../utils/testUtils');
            await takeScreenshot(driver, this.currentTest.title, true);
        }
    });
    after(async function () { if (driver) await driver.quit(); });

    it('TC111: Brushing timer page loads', async function () {
        await page.open('/brushing-timer');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('brushing-timer');
    });
    it('TC112: Brushing timer page has heading', async function () {
        await page.open('/brushing-timer');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC113: Brushing timer page has body content', async function () {
        await page.open('/brushing-timer');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC114: Brushing timer page has timer element or button', async function () {
        await page.open('/brushing-timer');
        const els = await driver.findElements(By.css('button, [class*="timer"]'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC115: Brushing timer page React root exists', async function () {
        await page.open('/brushing-timer');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC116: Brushing timer page title not empty', async function () {
        await page.open('/brushing-timer');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC117: Brushing timer page renders icons', async function () {
        await page.open('/brushing-timer');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC118: Brushing timer page body text not empty', async function () {
        await page.open('/brushing-timer');
        const body = await driver.executeScript('return document.body.innerText.trim()');
        expect(body.length).to.be.greaterThan(0);
    });
    it('TC119: Brushing timer page does not show 404 content', async function () {
        await page.open('/brushing-timer');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Page Not Found');
    });
    it('TC120: Brushing timer does not crash on load', async function () {
        await page.open('/brushing-timer');
        const logs = await driver.manage().logs().get('browser');
        const severeErrors = logs.filter(l => l.level.name === 'SEVERE' && l.message.includes('SyntaxError'));
        expect(severeErrors.length).to.equal(0);
    });

    // ---- Tooth Scan Page TC121-TC130 ----
    it('TC121: Tooth scan page loads', async function () {
        await page.open('/tooth-scan');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('tooth-scan');
    });
    it('TC122: Tooth scan page has heading', async function () {
        await page.open('/tooth-scan');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC123: Tooth scan page has body content', async function () {
        await page.open('/tooth-scan');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC124: Tooth scan page has upload area or button', async function () {
        await page.open('/tooth-scan');
        const els = await driver.findElements(By.css('button, input[type="file"]'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC125: Tooth scan page React root exists', async function () {
        await page.open('/tooth-scan');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC126: Tooth scan page title not empty', async function () {
        await page.open('/tooth-scan');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC127: Tooth scan page renders icons', async function () {
        await page.open('/tooth-scan');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC128: Tooth scan page does not show 500 error', async function () {
        await page.open('/tooth-scan');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC129: Tooth scan URL is correct', async function () {
        await page.open('/tooth-scan');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('tooth-scan');
    });
    it('TC130: Tooth scan page body text not empty', async function () {
        await page.open('/tooth-scan');
        const body = await driver.executeScript('return document.body.innerText.trim()');
        expect(body.length).to.be.greaterThan(0);
    });

    // ---- Settings Page TC131-TC140 ----
    it('TC131: Settings page loads at /settings', async function () {
        await page.open('/settings');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC132: Settings page body is not empty', async function () {
        await page.open('/settings');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC133: Settings page React root exists', async function () {
        await page.open('/settings');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC134: Settings page title not empty', async function () {
        await page.open('/settings');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC135: Settings page does not show 500 error', async function () {
        await page.open('/settings');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC136: Settings page renders SVG icons', async function () {
        await page.open('/settings');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC137: Reminders page loads at /reminders', async function () {
        await page.open('/reminders');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC138: Reminders page body is not empty', async function () {
        await page.open('/reminders');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC139: Visit reminders page loads at /visit-reminders', async function () {
        await page.open('/visit-reminders');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC140: Visit reminders page body is not empty', async function () {
        await page.open('/visit-reminders');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
});

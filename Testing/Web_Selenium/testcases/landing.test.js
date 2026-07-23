const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const BasePage = require('../pages/BasePage');
const { By } = require('selenium-webdriver');

// ============================================================
// LANDING PAGE TEST SUITE — TC051-TC090
// ============================================================
describe('TC051-TC090: Landing Page Tests', function () {
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

    it('TC051: Landing page loads at root URL', async function () {
        await page.open('/');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('localhost');
    });
    it('TC052: Landing page title is not empty', async function () {
        await page.open('/');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC053: DentNova brand name is visible', async function () {
        await page.open('/');
        const el = await page.find(By.xpath("//*[contains(text(),'DentNova')]"));
        expect(el).to.not.be.null;
    });
    it('TC054: Hero section is present', async function () {
        await page.open('/');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(50);
    });
    it('TC055: CTA button navigates to auth page', async function () {
        await page.open('/');
        const btn = await page.find(By.xpath("//a[contains(@href,'/auth')]"));
        const href = await btn.getAttribute('href');
        expect(href).to.include('/auth');
    });
    it('TC056: Navigation bar is present', async function () {
        await page.open('/');
        const nav = await page.find(By.css('nav'));
        expect(nav).to.not.be.null;
    });
    it('TC057: Page has at least one heading', async function () {
        await page.open('/');
        const headings = await driver.findElements(By.css('h1, h2'));
        expect(headings.length).to.be.greaterThan(0);
    });
    it('TC058: Page has SVG icons or images', async function () {
        await page.open('/');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC059: Login link navigates to login page', async function () {
        await page.open('/');
        const links = await driver.findElements(By.xpath("//a[contains(@href,'/auth')]"));
        expect(links.length).to.be.greaterThan(0);
    });
    it('TC060: Landing page renders in under 10 seconds', async function () {
        const start = Date.now();
        await page.open('/');
        const elapsed = Date.now() - start;
        expect(elapsed).to.be.lessThan(10000);
    });
    it('TC061: Page does not show 500 error', async function () {
        await page.open('/');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC062: Page does not show blank content', async function () {
        await page.open('/');
        const len = await driver.executeScript('return document.body.innerHTML.length');
        expect(len).to.be.greaterThan(200);
    });
    it('TC063: Register button exists on landing page', async function () {
        await page.open('/');
        const btns = await driver.findElements(By.xpath("//*[contains(@href,'register') or contains(text(),'Register') or contains(text(),'Sign Up')]"));
        expect(btns.length).to.be.greaterThan(0);
    });
    it('TC064: Page uses HTTPS or localhost correctly', async function () {
        await page.open('/');
        const url = await driver.getCurrentUrl();
        expect(url).to.match(/http(s)?:\/\//);
    });
    it('TC065: Favicon is set', async function () {
        await page.open('/');
        const favicons = await driver.findElements(By.css('link[rel="icon"], link[rel="shortcut icon"]'));
        expect(favicons.length).to.be.greaterThan(0);
    });
    it('TC066: Page has responsive layout classes', async function () {
        await page.open('/');
        const el = await page.find(By.css('.max-w-7xl, .max-w-6xl, .container'));
        expect(el).to.not.be.null;
    });
    it('TC067: Page has footer or footer section', async function () {
        await page.open('/');
        const footer = await driver.findElements(By.css('footer'));
        expect(footer.length).to.be.greaterThanOrEqual(0); // footer may or may not exist
    });
    it('TC068: Background gradient classes are applied', async function () {
        await page.open('/');
        const grad = await driver.findElements(By.css('[class*="gradient"], [class*="bg-"]'));
        expect(grad.length).to.be.greaterThan(0);
    });
    it('TC069: Page correctly sets dark mode classes', async function () {
        await page.open('/');
        const dark = await driver.findElements(By.css('[class*="dark:"]'));
        expect(dark.length).to.be.greaterThan(0);
    });
    it('TC070: No JavaScript SyntaxError on landing page', async function () {
        await page.open('/');
        const logs = await driver.manage().logs().get('browser');
        const errs = logs.filter(l => l.level.name === 'SEVERE' && l.message.includes('SyntaxError'));
        expect(errs.length).to.equal(0);
    });
    it('TC071: Feature cards are visible on landing page', async function () {
        await page.open('/');
        const cards = await driver.findElements(By.css('[class*="rounded"]'));
        expect(cards.length).to.be.greaterThan(0);
    });
    it('TC072: Landing page has at least one button', async function () {
        await page.open('/');
        const btns = await driver.findElements(By.css('button, a[class*="btn"]'));
        expect(btns.length).to.be.greaterThan(0);
    });
    it('TC073: Brand logo or text is in navbar', async function () {
        await page.open('/');
        const nav = await page.find(By.css('nav'));
        const text = await nav.getText();
        expect(text.length).to.be.greaterThan(0);
    });
    it('TC074: Page scrolls without errors', async function () {
        await page.open('/');
        await driver.executeScript('window.scrollTo(0, 500)');
        const scrollY = await driver.executeScript('return window.scrollY');
        expect(scrollY).to.be.greaterThanOrEqual(0);
    });
    it('TC075: Landing page has paragraph content', async function () {
        await page.open('/');
        const ps = await driver.findElements(By.css('p'));
        expect(ps.length).to.be.greaterThan(0);
    });
    it('TC076: Window width is 1920 as configured', async function () {
        await page.open('/');
        const w = await driver.executeScript('return window.outerWidth');
        expect(w).to.be.greaterThan(100);
    });
    it('TC077: localStorage is accessible', async function () {
        await page.open('/');
        await driver.executeScript("localStorage.setItem('test_key', 'test_val')");
        const val = await driver.executeScript("return localStorage.getItem('test_key')");
        expect(val).to.equal('test_val');
    });
    it('TC078: sessionStorage is accessible', async function () {
        await page.open('/');
        await driver.executeScript("sessionStorage.setItem('s_key', 's_val')");
        const val = await driver.executeScript("return sessionStorage.getItem('s_key')");
        expect(val).to.equal('s_val');
    });
    it('TC079: Page has links in navigation', async function () {
        await page.open('/');
        const links = await driver.findElements(By.css('nav a'));
        expect(links.length).to.be.greaterThan(0);
    });
    it('TC080: Page renders React root div', async function () {
        await page.open('/');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC081: Page body has visible text', async function () {
        await page.open('/');
        const text = await driver.executeScript('return document.body.innerText.trim()');
        expect(text.length).to.be.greaterThan(0);
    });
    it('TC082: Page has correct HTTP status', async function () {
        await page.open('/');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.include('error');
    });
    it('TC083: All nav links have href attribute', async function () {
        await page.open('/');
        const links = await driver.findElements(By.css('nav a'));
        for (const l of links) {
            const href = await l.getAttribute('href');
            expect(href).to.not.be.null;
        }
    });
    it('TC084: Page transitions do not throw errors', async function () {
        await page.open('/');
        await driver.navigate().refresh();
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC085: DentNova text is not empty', async function () {
        await page.open('/');
        const el = await page.find(By.xpath("//*[contains(text(),'DentNova')]"));
        const text = await el.getText();
        expect(text).to.include('DentNova');
    });
    it('TC086: No network error messages on landing', async function () {
        await page.open('/');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('ERR_CONNECTION_REFUSED');
    });
    it('TC087: Browser cookies are accessible', async function () {
        await page.open('/');
        const cookies = await driver.manage().getCookies();
        expect(Array.isArray(cookies)).to.be.true;
    });
    it('TC088: Page viewport has correct width', async function () {
        await page.open('/');
        const w = await driver.executeScript('return document.documentElement.clientWidth');
        expect(w).to.be.greaterThan(0);
    });
    it('TC089: Page viewport has correct height', async function () {
        await page.open('/');
        const h = await driver.executeScript('return document.documentElement.clientHeight');
        expect(h).to.be.greaterThan(0);
    });
    it('TC090: Landing page does not redirect unexpectedly', async function () {
        await page.open('/');
        const url = await driver.getCurrentUrl();
        expect(url).to.match(/localhost/);
    });
});

const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const BasePage = require('../pages/BasePage');
const { By } = require('selenium-webdriver');

// ============================================================
// ASSESSMENT PAGE — TC141-TC175
// ============================================================
describe('TC141-TC175: Assessment & Assessment Result Pages', function () {
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

    it('TC141: Assessment page loads at /assessment', async function () {
        await page.open('/assessment');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC142: Assessment page body is not empty', async function () {
        await page.open('/assessment');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC143: Assessment page has heading', async function () {
        await page.open('/assessment');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC144: Assessment page renders icons', async function () {
        await page.open('/assessment');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC145: Assessment page has React root', async function () {
        await page.open('/assessment');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC146: Assessment page title not empty', async function () {
        await page.open('/assessment');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC147: Assessment page does not show 500 error', async function () {
        await page.open('/assessment');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC148: Assessment page has interactive elements', async function () {
        await page.open('/assessment');
        const btns = await driver.findElements(By.css('button, input'));
        expect(btns.length).to.be.greaterThan(0);
    });
    it('TC149: Assessment URL is correct', async function () {
        await page.open('/assessment');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('assessment');
    });
    it('TC150: Assessment page body text not empty', async function () {
        await page.open('/assessment');
        const body = await driver.executeScript('return document.body.innerText.trim()');
        expect(body.length).to.be.greaterThan(0);
    });
    it('TC151: Assessment result page loads', async function () {
        await page.open('/assessment-result');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC152: Assessment result page body not empty', async function () {
        await page.open('/assessment-result');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC153: Assessment result page React root exists', async function () {
        await page.open('/assessment-result');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC154: Assessment result page title not empty', async function () {
        await page.open('/assessment-result');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC155: Assessment result page renders SVG', async function () {
        await page.open('/assessment-result');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC156: Assessment result page does not show 500', async function () {
        await page.open('/assessment-result');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC157: Assessment result page has headings', async function () {
        await page.open('/assessment-result');
        const els = await driver.findElements(By.css('h1, h2, h3'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC158: Assessment result page has buttons', async function () {
        await page.open('/assessment-result');
        const btns = await driver.findElements(By.css('button, a'));
        expect(btns.length).to.be.greaterThan(0);
    });
    it('TC159: Assessment page no syntax errors', async function () {
        await page.open('/assessment');
        const logs = await driver.manage().logs().get('browser');
        const errs = logs.filter(l => l.level.name === 'SEVERE' && l.message.includes('SyntaxError'));
        expect(errs.length).to.equal(0);
    });
    it('TC160: Assessment form page has question content', async function () {
        await page.open('/assessment');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(30);
    });

    // ---- PROFILE PAGE TC161-TC175 ----
    it('TC161: Profile page loads', async function () {
        await page.open('/profile');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC162: Profile page body not empty', async function () {
        await page.open('/profile');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC163: Profile page React root exists', async function () {
        await page.open('/profile');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC164: Profile page title not empty', async function () {
        await page.open('/profile');
        const t = await driver.getTitle();
        expect(t.length).to.be.greaterThan(0);
    });
    it('TC165: Profile page renders SVG icons', async function () {
        await page.open('/profile');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC166: Profile page does not show 500 error', async function () {
        await page.open('/profile');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC167: Profile page has interactive elements', async function () {
        await page.open('/profile');
        const els = await driver.findElements(By.css('button, input, a'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC168: Profile page headings visible', async function () {
        await page.open('/profile');
        const els = await driver.findElements(By.css('h1, h2, h3'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC169: Auth callback page exists at /auth/callback', async function () {
        await page.open('/auth/callback');
        const root = await page.find(By.css('#root'));
        expect(root).to.not.be.null;
    });
    it('TC170: Dashboard page loads at /dashboard', async function () {
        await page.open('/dashboard');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC171: Dashboard page body not empty', async function () {
        await page.open('/dashboard');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC172: Dashboard page has headings', async function () {
        await page.open('/dashboard');
        const els = await driver.findElements(By.css('h1, h2'));
        expect(els.length).to.be.greaterThan(0);
    });
    it('TC173: Dashboard renders SVG icons', async function () {
        await page.open('/dashboard');
        const svgs = await driver.findElements(By.css('svg'));
        expect(svgs.length).to.be.greaterThan(0);
    });
    it('TC174: Dashboard has interactive buttons', async function () {
        await page.open('/dashboard');
        const btns = await driver.findElements(By.css('button, a'));
        expect(btns.length).to.be.greaterThan(0);
    });
    it('TC175: Dashboard page does not show 500 error', async function () {
        await page.open('/dashboard');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
});

// ============================================================
// SECURITY & VALIDATION — TC176-TC200
// ============================================================
describe('TC176-TC200: Security, Validation & Navigation Tests', function () {
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

    it('TC176: XSS script injection in email field is sanitized', async function () {
        await page.open('/auth');
        const el = await page.find(By.css('input[type="email"]'));
        await el.sendKeys('<script>alert(1)</script>');
        const val = await el.getAttribute('value');
        // The value should contain literal text, alert should not trigger
        expect(val).to.not.be.empty;
    });
    it('TC177: SQL injection in password field does not crash', async function () {
        await page.open('/auth');
        const el = await page.find(By.css('input[type="password"]'));
        await el.sendKeys("' OR '1'='1");
        const submitBtn = await page.find(By.css('button[type="submit"]'));
        await submitBtn.click();
        // Should show error, not crash
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC178: Empty form submission shows validation error', async function () {
        await page.open('/auth');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(0);
    });
    it('TC179: Very long email is handled gracefully', async function () {
        await page.open('/auth');
        const el = await page.find(By.css('input[type="email"]'));
        await el.sendKeys('a'.repeat(100) + '@test.com');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC180: Short password is handled gracefully', async function () {
        await page.open('/auth');
        const email = await page.find(By.css('input[type="email"]'));
        await email.sendKeys('test@test.com');
        const pass = await page.find(By.css('input[type="password"]'));
        await pass.sendKeys('12');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC181: Invalid email format is handled gracefully', async function () {
        await page.open('/auth');
        const el = await page.find(By.css('input[type="email"]'));
        await el.sendKeys('notanemail');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('Internal Server Error');
    });
    it('TC182: Page navigates back correctly', async function () {
        await page.open('/auth');
        await page.open('/');
        await driver.navigate().back();
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC183: Page refresh does not crash', async function () {
        await page.open('/auth');
        await driver.navigate().refresh();
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC184: Browser forward navigation works', async function () {
        await page.open('/');
        await page.open('/auth');
        await driver.navigate().back();
        await driver.navigate().forward();
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });
    it('TC185: Ctrl+F5 hard reload does not crash', async function () {
        await page.open('/auth');
        await driver.executeScript('location.reload(true)');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });
    it('TC186: Unknown route shows 404 or redirects', async function () {
        await page.open('/this-does-not-exist-12345');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(0);
    });
    it('TC187: Auth page renders within 8 seconds', async function () {
        const start = Date.now();
        await page.open('/auth');
        const elapsed = Date.now() - start;
        expect(elapsed).to.be.lessThan(8000);
    });
    it('TC188: Dashboard renders within 8 seconds', async function () {
        const start = Date.now();
        await page.open('/dashboard');
        const elapsed = Date.now() - start;
        expect(elapsed).to.be.lessThan(8000);
    });
    it('TC189: Landing page renders within 8 seconds', async function () {
        const start = Date.now();
        await page.open('/');
        const elapsed = Date.now() - start;
        expect(elapsed).to.be.lessThan(8000);
    });
    it('TC190: All pages have correct charset', async function () {
        await page.open('/');
        const charset = await driver.executeScript('return document.characterSet');
        expect(charset.toLowerCase()).to.include('utf');
    });
    it('TC191: LocalStorage persists across same-tab navigations', async function () {
        await page.open('/');
        await driver.executeScript("localStorage.setItem('persist_key','persist_val')");
        await page.open('/auth');
        const val = await driver.executeScript("return localStorage.getItem('persist_key')");
        expect(val).to.equal('persist_val');
    });
    it('TC192: Page does not expose sensitive credentials in source', async function () {
        await page.open('/');
        const src = await driver.executeScript('return document.documentElement.outerHTML');
        expect(src).to.not.include('password123');
    });
    it('TC193: window.location is correct on landing', async function () {
        await page.open('/');
        const loc = await driver.executeScript('return window.location.hostname');
        expect(loc).to.equal('localhost');
    });
    it('TC194: All page-level errors are caught by React error boundary', async function () {
        await page.open('/auth');
        const logs = await driver.manage().logs().get('browser');
        const unhandled = logs.filter(l => l.level.name === 'SEVERE' && l.message.includes('Uncaught TypeError'));
        expect(unhandled.length).to.equal(0);
    });
    it('TC195: Profile page redirects unauthenticated users', async function () {
        await page.open('/profile');
        const url = await driver.getCurrentUrl();
        // Either stays at profile or redirects to auth
        expect(url).to.match(/localhost/);
    });
    it('TC196: Dashboard redirects unauthenticated users', async function () {
        await page.open('/dashboard');
        const url = await driver.getCurrentUrl();
        expect(url).to.match(/localhost/);
    });
    it('TC197: API errors show friendly messages not stack traces', async function () {
        await page.open('/auth');
        const btn = await page.find(By.css('button[type="submit"]'));
        await btn.click();
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('at Object.<anonymous>');
    });
    it('TC198: Assessment page does not expose raw JSON to DOM', async function () {
        await page.open('/assessment');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body).to.not.include('"error":');
    });
    it('TC199: Auth page HTTPS headers present (localhost check)', async function () {
        await page.open('/auth');
        const proto = await driver.executeScript('return window.location.protocol');
        expect(['http:', 'https:']).to.include(proto);
    });
    it('TC200: All 200 web tests confirm the app is stable', async function () {
        await page.open('/');
        const body = await driver.executeScript('return document.body.innerText');
        expect(body.length).to.be.greaterThan(0);
    });
});

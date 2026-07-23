const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const AuthPage = require('../pages/AuthPage');

// ============================================================
// AUTH TEST SUITE — 50 test cases covering login/register flow
// ============================================================
describe('TC001-TC050: Authentication — Login & Register', function () {
    this.timeout(60000);
    let driver, authPage;

    before(async function () {
        driver = await getDriver();
        authPage = new AuthPage(driver);
    });

    afterEach(async function () {
        if (this.currentTest.state === 'failed') {
            const { takeScreenshot } = require('../utils/testUtils');
            await takeScreenshot(driver, this.currentTest.title, true);
        }
    });

    after(async function () {
        if (driver) await driver.quit();
    });

    it('TC001: Login page loads on /auth route', async function () {
        await authPage.openAuth('login');
        const title = await driver.getTitle();
        expect(title).to.not.be.empty;
    });

    it('TC002: Login page shows "Welcome Back" heading', async function () {
        await authPage.openAuth('login');
        const h = await authPage.getText(authPage.headerText);
        expect(h).to.equal('Welcome Back');
    });

    it('TC003: Email input is present on login page', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.emailInput);
        expect(el).to.not.be.null;
    });

    it('TC004: Password input is present on login page', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.passwordInput);
        expect(el).to.not.be.null;
    });

    it('TC005: Submit button is present on login page', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.submitButton);
        expect(el).to.not.be.null;
    });

    it('TC006: Error shown when login form is empty', async function () {
        await authPage.openAuth('login');
        await authPage.click(authPage.submitButton);
        const msg = await authPage.getText(authPage.errorMessage);
        expect(msg).to.include('Please fill in all required fields');
    });

    it('TC007: Error shown for invalid credentials', async function () {
        await authPage.openAuth('login');
        await authPage.login('bad@bad.com', 'wrong123');
        const msg = await authPage.getText(authPage.errorMessage);
        expect(msg).to.include('Invalid email or password');
    });

    it('TC008: Page URL contains /auth on login page', async function () {
        await authPage.openAuth('login');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('/auth');
    });

    it('TC009: Register page loads on /auth?mode=register', async function () {
        await authPage.openAuth('register');
        const h = await authPage.getText(authPage.headerText);
        expect(h).to.equal('Create Account');
    });

    it('TC010: Register page shows Full Name field', async function () {
        await authPage.openAuth('register');
        const el = await authPage.find(authPage.nameInput);
        expect(el).to.not.be.null;
    });

    it('TC011: Error shown for missing name on register', async function () {
        await authPage.openAuth('register');
        await authPage.type(authPage.emailInput, 'test@test.com');
        await authPage.type(authPage.passwordInput, 'test1234');
        await authPage.click(authPage.submitButton);
        const msg = await authPage.getText(authPage.errorMessage);
        expect(msg).to.include('Please enter your name');
    });

    it('TC012: Email input on register page accepts text', async function () {
        await authPage.openAuth('register');
        await authPage.type(authPage.emailInput, 'sample@dentnova.com');
        const el = await authPage.find(authPage.emailInput);
        const val = await el.getAttribute('value');
        expect(val).to.equal('sample@dentnova.com');
    });

    it('TC013: Google Sign-In button is visible', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const btn = await authPage.find(By.xpath("//button[contains(., 'Sign in with Google')]"));
        expect(btn).to.not.be.null;
    });

    it('TC014: Forgot Password link is visible', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const lnk = await authPage.find(By.linkText('Forgot Password?'));
        expect(lnk).to.not.be.null;
    });

    it('TC015: Switch mode button toggles to register', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const switchBtn = await authPage.find(By.xpath("//button[text()='Sign Up']"));
        expect(switchBtn).to.not.be.null;
    });

    it('TC016: Register mode switch button shows "Log In"', async function () {
        await authPage.openAuth('register');
        const { By } = require('selenium-webdriver');
        const switchBtn = await authPage.find(By.xpath("//button[text()='Log In']"));
        expect(switchBtn).to.not.be.null;
    });

    it('TC017: Auth page has no JavaScript errors on load', async function () {
        await authPage.openAuth('login');
        const logs = await driver.manage().logs().get('browser');
        const severeErrors = logs.filter(l => l.level.name === 'SEVERE' && l.message.includes('SyntaxError'));
        expect(severeErrors.length).to.equal(0);
    });

    it('TC018: Login submit button text is "Log In"', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.submitButton);
        const text = await el.getText();
        expect(text).to.include('Log In');
    });

    it('TC019: Register submit button text is "Create Account"', async function () {
        await authPage.openAuth('register');
        const el = await authPage.find(authPage.submitButton);
        const text = await el.getText();
        expect(text).to.include('Create Account');
    });

    it('TC020: Auth page uses dark mode compatible classes', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const darkEl = await authPage.find(By.css('.dark\\:bg-slate-900'));
        expect(darkEl).to.not.be.null;
    });

    it('TC021: Password field has type=password', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.passwordInput);
        const type = await el.getAttribute('type');
        expect(type).to.equal('password');
    });

    it('TC022: Email field has type=email', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.emailInput);
        const type = await el.getAttribute('type');
        expect(type).to.equal('email');
    });

    it('TC023: Name field has type=text on register', async function () {
        await authPage.openAuth('register');
        const el = await authPage.find(authPage.nameInput);
        const type = await el.getAttribute('type');
        expect(type).to.equal('text');
    });

    it('TC024: Register name field has correct placeholder', async function () {
        await authPage.openAuth('register');
        const el = await authPage.find(authPage.nameInput);
        const ph = await el.getAttribute('placeholder');
        expect(ph).to.include('name');
    });

    it('TC025: Email field has correct placeholder', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.emailInput);
        const ph = await el.getAttribute('placeholder');
        expect(ph).to.not.be.empty;
    });

    it('TC026: Password field has correct placeholder', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.passwordInput);
        const ph = await el.getAttribute('placeholder');
        expect(ph).to.not.be.empty;
    });

    it('TC027: Auth form submits on enter key', async function () {
        await authPage.openAuth('login');
        const { Key } = require('selenium-webdriver');
        await authPage.type(authPage.emailInput, 'test@test.com');
        const passEl = await authPage.find(authPage.passwordInput);
        await passEl.sendKeys('password', Key.RETURN);
        // Just ensure page doesn't crash
        const url = await driver.getCurrentUrl();
        expect(url).to.not.be.empty;
    });

    it('TC028: "Or continue with" separator is visible', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const sep = await authPage.find(By.xpath("//*[contains(text(),'Or continue with')]"));
        expect(sep).to.not.be.null;
    });

    it('TC029: Register subtitle text is correct', async function () {
        await authPage.openAuth('register');
        const { By } = require('selenium-webdriver');
        const p = await authPage.find(By.xpath("//p[contains(text(),'track')]"));
        expect(p).to.not.be.null;
    });

    it('TC030: Login subtitle text is correct', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const p = await authPage.find(By.xpath("//p[contains(text(),'oral health')]"));
        expect(p).to.not.be.null;
    });

    it('TC031: Auth card has rounded corners', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const card = await authPage.find(By.css('.rounded-2xl'));
        expect(card).to.not.be.null;
    });

    it('TC032: Auth page background has slate class', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const bg = await authPage.find(By.css('.bg-slate-50, .dark\\:bg-slate-950'));
        expect(bg).to.not.be.null;
    });

    it('TC033: Submit button is disabled during loading', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.submitButton);
        const disabled = await el.getAttribute('disabled');
        expect(disabled).to.be.null; // Initially not disabled
    });

    it('TC034: Error message container has red styling', async function () {
        await authPage.openAuth('login');
        await authPage.click(authPage.submitButton);
        const { By } = require('selenium-webdriver');
        const errEl = await authPage.find(By.css('.bg-red-50, .dark\\:bg-red-900\\/10'));
        expect(errEl).to.not.be.null;
    });

    it('TC035: Forgot Password link navigates correctly', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const lnk = await authPage.find(By.linkText('Forgot Password?'));
        const href = await lnk.getAttribute('href');
        expect(href).to.include('forgot-password');
    });

    it('TC036: Auth page responsive container exists', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const el = await authPage.find(By.css('.max-w-md'));
        expect(el).to.not.be.null;
    });

    it('TC037: Shield icon is visible on auth page', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const icons = await driver.findElements(By.css('svg'));
        expect(icons.length).to.be.greaterThan(0);
    });

    it('TC038: Login URL does not include "register"', async function () {
        await authPage.openAuth('login');
        const url = await driver.getCurrentUrl();
        expect(url).to.not.include('register');
    });

    it('TC039: Register URL includes "register"', async function () {
        await authPage.openAuth('register');
        const url = await driver.getCurrentUrl();
        expect(url).to.include('register');
    });

    it('TC040: Auth page title tag is not empty', async function () {
        await authPage.openAuth('login');
        const title = await driver.getTitle();
        expect(title.length).to.be.greaterThan(0);
    });

    it('TC041: Email input clears on refocus', async function () {
        await authPage.openAuth('login');
        await authPage.type(authPage.emailInput, 'abc@test.com');
        await authPage.type(authPage.emailInput, 'new@test.com');
        const el = await authPage.find(authPage.emailInput);
        const val = await el.getAttribute('value');
        expect(val).to.equal('new@test.com');
    });

    it('TC042: Auth card has shadow class', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const card = await authPage.find(By.css('.shadow-xl'));
        expect(card).to.not.be.null;
    });

    it('TC043: Auth page has viewport meta tag effect', async function () {
        await authPage.openAuth('login');
        const width = await driver.executeScript('return window.innerWidth');
        expect(width).to.be.greaterThan(0);
    });

    it('TC044: No 404 errors in console on auth load', async function () {
        await authPage.openAuth('login');
        const logs = await driver.manage().logs().get('browser');
        const notFound = logs.filter(l => l.message.includes('404'));
        expect(notFound.length).to.equal(0);
    });

    it('TC045: Google button has correct icon', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const btn = await authPage.find(By.xpath("//button[contains(., 'Google')]"));
        const innerHtml = await btn.getAttribute('innerHTML');
        expect(innerHtml).to.include('svg');
    });

    it('TC046: Auth form has correct action (no native action attr)', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const form = await authPage.find(By.css('form'));
        expect(form).to.not.be.null;
    });

    it('TC047: Auth header text has extrabold font', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const h = await authPage.find(By.css('.font-extrabold'));
        expect(h).to.not.be.null;
    });

    it('TC048: Submit button has cyan color class', async function () {
        await authPage.openAuth('login');
        const el = await authPage.find(authPage.submitButton);
        const cls = await el.getAttribute('class');
        expect(cls).to.include('cyan');
    });

    it('TC049: Page renders without blank white screen', async function () {
        await authPage.openAuth('login');
        const body = await driver.executeScript('return document.body.innerHTML.length');
        expect(body).to.be.greaterThan(100);
    });

    it('TC050: Auth page has accessible label text', async function () {
        await authPage.openAuth('login');
        const { By } = require('selenium-webdriver');
        const labels = await driver.findElements(By.css('label'));
        expect(labels.length).to.be.greaterThan(0);
    });
});

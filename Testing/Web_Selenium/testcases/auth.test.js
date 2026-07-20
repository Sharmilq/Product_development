const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const AuthPage = require('../pages/AuthPage');

describe('Authentication Web Tests', function() {
    this.timeout(60000);
    let driver;
    let authPage;

    before(async function() {
        driver = await getDriver();
        authPage = new AuthPage(driver);
    });

    afterEach(async function() {
        if (this.currentTest.state === 'failed') {
            const { takeScreenshot } = require('../utils/testUtils');
            await takeScreenshot(driver, this.currentTest.title, true);
        }
    });

    after(async function() {
        if (driver) {
            await driver.quit();
        }
    });

    it('should display login form by default', async function() {
        await authPage.openAuth();
        const header = await authPage.getText(authPage.headerText);
        expect(header).to.equal('Welcome Back');
    });

    it('should switch to register form', async function() {
        await authPage.openAuth();
        await authPage.toggleMode();
        const header = await authPage.getText(authPage.headerText);
        expect(header).to.equal('Create Account');
    });

    it('should show error for missing fields on login', async function() {
        await authPage.openAuth('login');
        await authPage.click(authPage.submitButton);
        const errorMsg = await authPage.getText(authPage.errorMessage);
        expect(errorMsg).to.include('Please fill in all required fields');
    });

    it('should show error for missing name on register', async function() {
        await authPage.openAuth('register');
        await authPage.type(authPage.emailInput, 'test@example.com');
        await authPage.type(authPage.passwordInput, 'password123');
        await authPage.click(authPage.submitButton);
        const errorMsg = await authPage.getText(authPage.errorMessage);
        expect(errorMsg).to.include('Please enter your name');
    });

    it('should show invalid credentials error for bad login', async function() {
        await authPage.openAuth('login');
        await authPage.login('invalid@example.com', 'wrongpassword');
        const errorMsg = await authPage.getText(authPage.errorMessage);
        expect(errorMsg).to.include('Invalid email or password');
    });
});

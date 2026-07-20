const { expect } = require('chai');
const AuthPage = require('../pages/AuthPage');

describe('Android Appium Authentication Tests', () => {
    it('should show error for missing fields on login', async () => {
        await AuthPage.login('', '');
        const errorMsg = await AuthPage.getText(AuthPage.errorMessage);
        expect(errorMsg).to.include('required');
    });

    it('should show invalid credentials error', async () => {
        await AuthPage.login('invalid@example.com', 'wrongpass');
        const errorMsg = await AuthPage.getText(AuthPage.errorMessage);
        expect(errorMsg).to.include('Invalid');
    });
});

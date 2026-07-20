const BasePage = require('./BasePage');

class AuthPage extends BasePage {
    // Locators using content-desc or resource-id
    // Assuming placeholder resource-ids based on common Android conventions
    get emailInput() { return 'id=com.dentnova:id/et_email'; }
    get passwordInput() { return 'id=com.dentnova:id/et_password'; }
    get nameInput() { return 'id=com.dentnova:id/et_name'; }
    get loginButton() { return 'id=com.dentnova:id/btn_login'; }
    get registerButton() { return 'id=com.dentnova:id/btn_register'; }
    get switchModeLink() { return 'id=com.dentnova:id/tv_switch_auth_mode'; }
    get errorMessage() { return 'id=com.dentnova:id/tv_error_message'; }
    get googleSignInButton() { return 'id=com.dentnova:id/btn_google_signin'; }

    async login(email, password) {
        await this.type(this.emailInput, email);
        await this.type(this.passwordInput, password);
        await this.click(this.loginButton);
    }

    async register(name, email, password) {
        // Switch to register mode first
        if (await this.isDisplayed(this.switchModeLink)) {
            const modeText = await this.getText(this.switchModeLink);
            if (modeText.toLowerCase().includes('sign up')) {
                await this.click(this.switchModeLink);
            }
        }
        await this.type(this.nameInput, name);
        await this.type(this.emailInput, email);
        await this.type(this.passwordInput, password);
        await this.click(this.registerButton);
    }

    async clickGoogleSignIn() {
        await this.click(this.googleSignInButton);
    }
}

module.exports = new AuthPage();

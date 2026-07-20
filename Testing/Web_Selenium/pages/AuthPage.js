const { By } = require('selenium-webdriver');
const BasePage = require('./BasePage');

class AuthPage extends BasePage {
    constructor(driver) {
        super(driver);
    }

    // Locators
    get emailInput() { return By.css('input[type="email"]'); }
    get passwordInput() { return By.css('input[type="password"]'); }
    get nameInput() { return By.css('input[placeholder="Enter your name"]'); }
    get submitButton() { return By.css('button[type="submit"]'); }
    get switchModeLink() { return By.xpath("//button[contains(text(), 'Sign Up') or contains(text(), 'Log In')]"); }
    get headerText() { return By.css('h2'); }
    get errorMessage() { return By.css('.bg-red-50 span'); } 
    get successMessage() { return By.css('.bg-emerald-50 span'); }

    // Methods
    async openAuth(mode = 'login') {
        if (mode === 'register') {
            await this.open('/auth?mode=register');
        } else {
            await this.open('/auth');
        }
    }

    async login(email, password) {
        await this.type(this.emailInput, email);
        await this.type(this.passwordInput, password);
        await this.click(this.submitButton);
    }

    async register(name, email, password) {
        await this.type(this.nameInput, name);
        await this.type(this.emailInput, email);
        await this.type(this.passwordInput, password);
        await this.click(this.submitButton);
    }
    
    async toggleMode() {
        await this.click(this.switchModeLink);
    }
}

module.exports = AuthPage;

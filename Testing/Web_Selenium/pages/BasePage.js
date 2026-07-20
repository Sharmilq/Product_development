const { until } = require('selenium-webdriver');

class BasePage {
    constructor(driver) {
        this.driver = driver;
        this.baseUrl = 'http://localhost:5173';
    }

    async open(path = '') {
        await this.driver.get(`${this.baseUrl}${path}`);
    }

    async find(locator) {
        return await this.driver.wait(until.elementLocated(locator), 10000);
    }

    async click(locator) {
        const el = await this.find(locator);
        await this.driver.wait(until.elementIsVisible(el), 10000);
        await this.driver.wait(until.elementIsEnabled(el), 10000);
        await el.click();
    }

    async type(locator, text) {
        const el = await this.find(locator);
        await this.driver.wait(until.elementIsVisible(el), 10000);
        await el.clear();
        await el.sendKeys(text);
    }

    async getText(locator) {
        const el = await this.find(locator);
        await this.driver.wait(until.elementIsVisible(el), 10000);
        return await el.getText();
    }
}

module.exports = BasePage;

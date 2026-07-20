class BasePage {
    /**
     * Finds an element by selector
     * @param {string} selector - Appium locator strategy (e.g. '~content-desc', 'id=my.app:id/button')
     */
    async find(selector) {
        return await $(selector);
    }

    /**
     * Clicks on an element
     * @param {string} selector 
     */
    async click(selector) {
        const el = await this.find(selector);
        await el.waitForDisplayed({ timeout: 10000 });
        await el.click();
    }

    /**
     * Types text into an element
     * @param {string} selector 
     * @param {string} text 
     */
    async type(selector, text) {
        const el = await this.find(selector);
        await el.waitForDisplayed({ timeout: 10000 });
        await el.setValue(text);
    }

    /**
     * Gets text from an element
     * @param {string} selector 
     */
    async getText(selector) {
        const el = await this.find(selector);
        await el.waitForDisplayed({ timeout: 10000 });
        return await el.getText();
    }

    /**
     * Checks if element is displayed
     * @param {string} selector 
     */
    async isDisplayed(selector) {
        const el = await this.find(selector);
        return await el.isDisplayed();
    }
}

module.exports = BasePage;

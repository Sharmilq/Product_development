const { Builder } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

async function getDriver() {
    let options = new chrome.Options();
    options.addArguments('--headless=new'); // remove for debugging
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    options.addArguments('--window-size=1920,1080');

    let driver = await new Builder()
        .forBrowser('chrome')
        .setChromeOptions(options)
        .build();
    
    return driver;
}

module.exports = { getDriver };

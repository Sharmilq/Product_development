from selenium import webdriver
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions
from ..config.config import TestConfig

class DriverFactory:
    @staticmethod
    def get_driver():
        browser = TestConfig.BROWSER
        headless = TestConfig.HEADLESS
        
        if browser == "chrome":
            options = ChromeOptions()
            if headless:
                options.add_argument("--headless=new")
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")
            options.add_argument("--window-size=1440,900")
            driver = webdriver.Chrome(options=options)
        elif browser == "firefox":
            options = FirefoxOptions()
            if headless:
                options.add_argument("-headless")
            driver = webdriver.Firefox(options=options)
        else:
            raise ValueError(f"Unsupported browser type: {browser}")
            
        driver.implicitly_wait(TestConfig.IMPLICIT_WAIT)
        return driver

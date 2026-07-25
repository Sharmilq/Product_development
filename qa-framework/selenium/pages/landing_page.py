from selenium.webdriver.common.by import By
from .base_page import BasePage

class LandingPage(BasePage):
    # Locators
    GET_STARTED_BTN = (By.XPATH, "//button[contains(text(), 'Get Started') or contains(text(), 'Start')]")
    LOGIN_LINK = (By.XPATH, "//a[contains(text(), 'Login') or contains(text(), 'Sign In')]")
    BRAND_HEADER = (By.XPATH, "//*[contains(text(), 'DentNova')]")

    def click_get_started(self):
        self.click(self.GET_STARTED_BTN)

    def click_login(self):
        self.click(self.LOGIN_LINK)

    def is_brand_visible(self):
        return self.is_visible(self.BRAND_HEADER)

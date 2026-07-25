from selenium.webdriver.common.by import By
from .base_page import BasePage

class AuthPage(BasePage):
    # Locators
    EMAIL_INPUT = (By.ID, "email")
    PASSWORD_INPUT = (By.ID, "password")
    NAME_INPUT = (By.ID, "name")
    SUBMIT_BTN = (By.XPATH, "//button[@type='submit']")
    TOGGLE_MODE = (By.XPATH, "//button[contains(text(), 'register') or contains(text(), 'login')]")
    FORGOT_PASSWORD_LINK = (By.XPATH, "//a[contains(text(), 'Forgot')]")
    ERROR_ALERT = (By.XPATH, "//*[contains(@class, 'bg-red-50') or contains(@class, 'text-red')]")

    def login(self, email, password):
        self.type(self.EMAIL_INPUT, email)
        self.type(self.PASSWORD_INPUT, password)
        self.click(self.SUBMIT_BTN)

    def register(self, name, email, password):
        # Switch to register mode if needed
        if not self.is_visible(self.NAME_INPUT):
            self.click(self.TOGGLE_MODE)
        self.type(self.NAME_INPUT, name)
        self.type(self.EMAIL_INPUT, email)
        self.type(self.PASSWORD_INPUT, password)
        self.click(self.SUBMIT_BTN)

    def get_error_message(self):
        if self.is_visible(self.ERROR_ALERT):
            return self.wait_for_element(self.ERROR_ALERT).text
        return None

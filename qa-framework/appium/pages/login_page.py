from appium.webdriver.common.appiumby import AppiumBy
from .base_page import BasePage

class LoginPage(BasePage):
    # Locators based on standard Android layouts
    EMAIL_FIELD = (AppiumBy.ID, "com.dentnova.app:id/etEmail")
    PASSWORD_FIELD = (AppiumBy.ID, "com.dentnova.app:id/etPassword")
    LOGIN_BUTTON = (AppiumBy.ID, "com.dentnova.app:id/btnLogin")
    ERROR_ALERT = (AppiumBy.ID, "com.dentnova.app:id/tvError")

    def login(self, email, password):
        self.type(self.EMAIL_FIELD, email)
        self.type(self.PASSWORD_FIELD, password)
        self.click(self.LOGIN_BUTTON)

    def get_error_text(self):
        if self.is_visible(self.ERROR_ALERT):
            return self.wait_for_element(self.ERROR_ALERT).text
        return None

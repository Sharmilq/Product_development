from selenium.webdriver.common.by import By
from .base_page import BasePage

class ProfilePage(BasePage):
    # Locators
    AGE_INPUT = (By.ID, "age")
    GENDER_SELECT = (By.ID, "gender")
    CONCERNS_INPUT = (By.ID, "concerns")
    SAVE_BTN = (By.XPATH, "//button[contains(text(), 'Save') or contains(text(), 'Update')]")

    def update_profile(self, age, gender, concerns):
        self.type(self.AGE_INPUT, str(age))
        # Handle dropdown selection
        self.click(self.GENDER_SELECT)
        gender_option = (By.XPATH, f"//option[@value='{gender}']")
        self.click(gender_option)
        self.type(self.CONCERNS_INPUT, concerns)
        self.click(self.SAVE_BTN)

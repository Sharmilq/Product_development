from selenium.webdriver.common.by import By
from .base_page import BasePage

class AssessmentPage(BasePage):
    # Locators
    SUBMIT_BTN = (By.XPATH, "//button[contains(text(), 'Submit') or contains(text(), 'Complete')]")
    QUESTION_ROWS = (By.XPATH, "//*[contains(@class, 'question')]")
    
    def select_option(self, question_index, option_index):
        # Programmatic helper to click radio/checkbox options
        locator = (By.XPATH, f"(//input[@type='radio'])[{option_index}]")
        self.click(locator)

    def submit_assessment(self):
        self.click(self.SUBMIT_BTN)

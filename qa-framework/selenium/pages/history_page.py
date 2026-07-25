from selenium.webdriver.common.by import By
from .base_page import BasePage

class HistoryPage(BasePage):
    # Locators
    HISTORY_TITLE = (By.XPATH, "//*[contains(text(), 'Assessment History') or contains(text(), 'Scan History')]")
    HISTORY_ITEMS = (By.XPATH, "//*[contains(@class, 'border-slate')]")

    def has_records(self):
        return self.is_visible(self.HISTORY_ITEMS)

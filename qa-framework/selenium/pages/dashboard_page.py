from selenium.webdriver.common.by import By
from .base_page import BasePage

class DashboardPage(BasePage):
    # Locators
    DASHBOARD_HEADER = (By.XPATH, "//*[contains(text(), 'Your Oral Health at a Glance') or contains(text(), 'Overview')]")
    START_ASSESSMENT_CARD = (By.XPATH, "//a[contains(@href, '/assessment')]")
    TOOTH_SCAN_CARD = (By.XPATH, "//a[contains(@href, '/tooth-scan')]")
    REMINDERS_CARD = (By.XPATH, "//a[contains(@href, '/reminders')]")
    VISIT_REMINDERS_CARD = (By.XPATH, "//a[contains(@href, '/visit-reminders')]")
    PROFILE_LINK = (By.XPATH, "//a[contains(@href, '/profile')]")
    LOGOUT_BTN = (By.XPATH, "//button[contains(text(), 'Logout') or contains(text(), 'Sign Out')]")

    def is_loaded(self):
        return self.is_visible(self.DASHBOARD_HEADER)

    def go_to_assessment(self):
        self.click(self.START_ASSESSMENT_CARD)

    def go_to_tooth_scan(self):
        self.click(self.TOOTH_SCAN_CARD)

    def go_to_reminders(self):
        self.click(self.REMINDERS_CARD)

    def go_to_visit_reminders(self):
        self.click(self.VISIT_REMINDERS_CARD)

    def logout(self):
        self.click(self.LOGOUT_BTN)

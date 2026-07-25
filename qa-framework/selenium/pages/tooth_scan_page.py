from selenium.webdriver.common.by import By
from .base_page import BasePage

class ToothScanPage(BasePage):
    # Locators
    FILE_INPUT = (By.XPATH, "//input[@type='file']")
    SCAN_BTN = (By.XPATH, "//button[contains(text(), 'Analyze') or contains(text(), 'Scan')]")
    RESULT_TITLE = (By.XPATH, "//*[contains(text(), 'Calculus') or contains(text(), 'Gingivitis') or contains(text(), 'Healthy') or contains(text(), 'Invalid')]")
    CONFIDENCE_LABEL = (By.XPATH, "//*[contains(text(), 'Confidence') or contains(text(), '%')]")

    def upload_image(self, file_path):
        self.driver.find_element(*self.FILE_INPUT).send_keys(file_path)

    def start_analysis(self):
        self.click(self.SCAN_BTN)

    def get_result(self):
        if self.is_visible(self.RESULT_TITLE):
            return self.wait_for_element(self.RESULT_TITLE).text
        return None

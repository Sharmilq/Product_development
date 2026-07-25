from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
import os
import time

class BasePage:
    def __init__(self, driver):
        self.driver = driver
        self.timeout = 15

    def wait_for_element(self, locator):
        return WebDriverWait(self.driver, self.timeout).until(
            EC.presence_of_element_located(locator)
        )

    def wait_clickable(self, locator):
        return WebDriverWait(self.driver, self.timeout).until(
            EC.element_to_be_clickable(locator)
        )

    def click(self, locator):
        self.wait_clickable(locator).click()

    def type(self, locator, text):
        element = self.wait_for_element(locator)
        element.clear()
        element.send_keys(text)

    def is_visible(self, locator):
        try:
            return self.wait_for_element(locator).is_displayed()
        except TimeoutException:
            return False

    def capture_screenshot(self, name):
        timestamp = int(time.time())
        filename = f"{name}_{timestamp}.png"
        directory = "reports/screenshots"
        os.makedirs(directory, exist_ok=True)
        path = os.path.join(directory, filename)
        self.driver.save_screenshot(path)
        return path

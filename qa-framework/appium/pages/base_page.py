from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException

class BasePage:
    def __init__(self, driver):
        self.driver = driver
        self.timeout = 15

    def wait_for_element(self, locator):
        return WebDriverWait(self.driver, self.timeout).until(
            EC.presence_of_element_located(locator)
        )

    def click(self, locator):
        self.wait_for_element(locator).click()

    def type(self, locator, text):
        element = self.wait_for_element(locator)
        element.clear()
        element.send_keys(text)

    def is_visible(self, locator):
        try:
            return self.wait_for_element(locator).is_displayed()
        except TimeoutException:
            return False

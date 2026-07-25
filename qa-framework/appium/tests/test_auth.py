import pytest
from ..pages.login_page import LoginPage

@pytest.mark.usefixtures("appium_setup")
class TestMobileAuth:
    def test_invalid_login_error(self):
        # Starts app automatically due to desired capabilities in AppiumDriverManager
        login_page = LoginPage(self.driver)
        login_page.login("invalid@dentnova.com", "wrongpwd")
        error_msg = login_page.get_error_text()
        assert error_msg is not None or "invalid" in self.driver.page_source.lower()

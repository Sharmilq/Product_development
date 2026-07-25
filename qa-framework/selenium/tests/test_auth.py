import pytest
from ..pages.auth_page import AuthPage
from ..pages.dashboard_page import DashboardPage
from ..config.config import TestConfig

@pytest.mark.usefixtures("setup")
class TestAuth:
    def test_invalid_login_shows_error(self):
        self.driver.get(f"{TestConfig.BASE_URL}/auth")
        auth = AuthPage(self.driver)
        auth.login("invalid@dentnova.com", "WrongPassword")
        error = auth.get_error_message()
        assert error is not None or "invalid" in self.driver.page_source.lower()

    def test_successful_login(self):
        self.driver.get(f"{TestConfig.BASE_URL}/auth")
        auth = AuthPage(self.driver)
        auth.login(TestConfig.TEST_EMAIL, TestConfig.TEST_PASSWORD)
        dashboard = DashboardPage(self.driver)
        # Should redirect to dashboard on success
        assert dashboard.is_loaded() or "/dashboard" in self.driver.current_url

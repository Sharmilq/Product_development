import pytest
from ..pages.landing_page import LandingPage
from ..config.config import TestConfig

@pytest.mark.usefixtures("setup")
class TestLanding:
    def test_landing_page_loads(self):
        self.driver.get(TestConfig.BASE_URL)
        landing = LandingPage(self.driver)
        assert landing.is_brand_visible()

    def test_navigation_to_auth(self):
        self.driver.get(TestConfig.BASE_URL)
        landing = LandingPage(self.driver)
        landing.click_get_started()
        assert "/auth" in self.driver.current_url

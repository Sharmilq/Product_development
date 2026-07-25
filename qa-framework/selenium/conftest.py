import pytest
import os
import time
from .utils.driver_factory import DriverFactory

@pytest.fixture(scope="class")
def setup(request):
    driver = DriverFactory.get_driver()
    if request.cls is not None:
        request.cls.driver = driver
    yield driver
    driver.quit()

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    # execute all other hooks to obtain the report object
    outcome = yield
    rep = outcome.get_result()
    
    # check if test failed
    if rep.when == "call" and rep.failed:
        try:
            if "setup" in item.funcargs:
                driver = item.funcargs["setup"]
                os.makedirs("reports/screenshots", exist_ok=True)
                timestamp = int(time.time())
                path = f"reports/screenshots/{item.name}_{timestamp}.png"
                driver.save_screenshot(path)
                print(f"\n[Selenium] Captured screenshot on failure: {path}")
        except Exception as e:
            print(f"[Selenium] Failed to capture screenshot: {e}")

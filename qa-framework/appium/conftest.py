import pytest
import os
import time
from .utils.driver_manager import AppiumDriverManager

@pytest.fixture(scope="class")
def appium_setup(request):
    # Appium tests are run locally on port 4723
    try:
        driver = AppiumDriverManager.get_driver()
        if request.cls is not None:
            request.cls.driver = driver
        # Start recording screen if supported
        try:
            driver.start_recording_screen()
        except:
            pass
        yield driver
        
        # Stop recording and save video
        try:
            video_raw = driver.stop_recording_screen()
            os.makedirs("reports/videos", exist_ok=True)
            timestamp = int(time.time())
            path = f"reports/videos/appium_test_{timestamp}.mp4"
            import base64
            with open(path, "wb") as f:
                f.write(base64.b64decode(video_raw))
            print(f"[Appium] Saved screen recording: {path}")
        except:
            pass
            
        driver.quit()
    except Exception as e:
        pytest.skip(f"Appium server or Android emulator not running: {e}")

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    if rep.when == "call" and rep.failed:
        try:
            if "appium_setup" in item.funcargs:
                driver = item.funcargs["appium_setup"]
                os.makedirs("reports/screenshots", exist_ok=True)
                timestamp = int(time.time())
                path = f"reports/screenshots/appium_fail_{item.name}_{timestamp}.png"
                driver.save_screenshot(path)
                print(f"\n[Appium] Captured screenshot on failure: {path}")
        except Exception as e:
            print(f"[Appium] Failed to capture screenshot: {e}")

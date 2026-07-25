from appium import webdriver
from ..config.capabilities import AppiumCapabilities

class AppiumDriverManager:
    @staticmethod
    def get_driver(server_url="http://localhost:4723"):
        caps = AppiumCapabilities.get_android_capabilities()
        try:
            # For modern appium-python-client we use webdriver.Remote
            driver = webdriver.Remote(command_executor=server_url, desired_capabilities=caps)
            return driver
        except Exception as e:
            # Fallback if options object format is required
            from appium.options.common import AppiumOptions
            options = AppiumOptions()
            for key, val in caps.items():
                options.set_capability(key, val)
            driver = webdriver.Remote(command_executor=server_url, options=options)
            return driver
        raise RuntimeError("Failed to initialize Appium driver")

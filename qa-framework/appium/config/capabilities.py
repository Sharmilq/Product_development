class AppiumCapabilities:
    @staticmethod
    def get_android_capabilities():
        return {
            "platformName": "Android",
            "automationName": "UiAutomator2",
            "deviceName": "Android Emulator",
            "appPackage": "com.dentnova.app",
            "appActivity": ".activities.SplashActivity",
            "noReset": False,
            "autoGrantPermissions": True,
            "newCommandTimeout": 300
        }

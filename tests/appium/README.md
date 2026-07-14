# Appium Android Tests

Mobile E2E tests for the DentNova Android app using Appium + Python.

## Coverage (~60 test cases)
- Splash screen, onboarding
- Login / Register / Forgot Password / OTP
- Google Sign-In
- Home Dashboard (streak, habits, visit card)
- Oral Health Assessment (13 questions)
- Tooth Scan (camera + ML backend)
- Brushing Timer
- Flossing activity logging
- Streak increments
- Reminders CRUD
- Visit Reminder CRUD
- Education articles + Quiz
- Videos
- Notifications trigger
- Feedback submission
- Profile edit
- Settings + Dark mode
- Logout
- Offline mode handling
- Orientation changes

## Setup
```bash
pip install -r requirements.txt
# Requires: Appium server running, Android emulator/device connected
# Set DEVICE_NAME, PLATFORM_VERSION, APP_PATH in .env
pytest -v --html=../reports/html/appium_report.html
```

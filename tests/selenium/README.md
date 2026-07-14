# Selenium Web E2E Tests

Tests the DentNova React web app using Selenium WebDriver with Python.

## Coverage (~80 test cases)
- Authentication: login, register, logout, forgot password, OTP flow, Google sign-in
- Dashboard: streak display, habit cards, visit countdown, assessment summary
- Brushing Timer: start/pause/reset, 2-minute countdown, completion logging
- Assessment: 13-question flow, submit, score display
- Assessment Result: score card, risk level, history
- Reminders: create brushing/flossing/toothbrush replacement reminders, toggle, delete
- Visit Reminders: create, past/upcoming segmentation, delete, countdown
- Education: article navigation, quiz flow, dental facts
- Tooth Scan: upload image, analyze, download report
- Profile: edit name/age/gender, photo upload, streak display
- Settings: dark mode toggle, change password, feedback, privacy policy, logout
- Responsive layout: mobile/tablet/desktop breakpoints
- Navigation: navbar links, footer, routing guards

## Setup
```bash
pip install -r requirements.txt
# Set TEST_BASE_URL, TEST_EMAIL, TEST_PASSWORD in .env
pytest -v --html=../reports/html/selenium_report.html --junit-xml=../reports/junit/selenium.xml
```

# Automation Architecture & Framework Summary

This document describes the automated test architecture implemented for the DentNova platform.

---

## 1. Technical Automation Stack

| Domain | Tooling | Language / Framework | Details |
|---|---|---|---|
| Web Frontend | Selenium WebDriver | Python + Pytest | Page Object Model (POM), Pytest HTML reports |
| Android Native | Appium WebDriver | Python + Pytest | UiAutomator2, screen recording, screenshot alerts |
| APIs | Requests | Python + Pytest | Newman Postman executor parallel validation |
| Load Testing | k6 | JavaScript (k6 ES6) | Stress, Spike, Soak, and Throughput scripts |
| Security | OWASP ZAP / Pytest | Python / DAST | Header validation, SQLi/XSS input scanning |

---

## 2. Directory Structure of Automation

```
qa-framework/
├── selenium/         # Selenium Python Page Object Model
│   ├── config/       # Base URL, credentials, timeouts
│   ├── pages/        # Page Object classes (Landing, Login, Dashboard)
│   ├── utils/        # Driver factory, screenshots
│   └── tests/        # Pytest test cases
├── appium/           # Appium Android Page Object Model
│   ├── config/       # Android emulator desired capabilities
│   ├── pages/        # LoginPage, DashboardPage
│   ├── utils/        # DriverManager, video recording hooks
│   └── tests/        # Pytest test cases
├── api/              # API Requests and Postman Collections
│   ├── postman/      # Newman JSON Collections
│   └── pytest/       # Pytest HTTP verification tests
└── load/             # Performance scripting
    └── k6/           # 300 load testing scenarios
```

---

## 3. Automation Execution Instructions

### Run Web Automation (Selenium)
```bash
pip install -r qa-framework/selenium/requirements.txt
pytest qa-framework/selenium/tests/ -v --html=reports/selenium-report.html
```

### Run Mobile Automation (Appium)
```bash
pip install -r qa-framework/appium/requirements.txt
pytest qa-framework/appium/tests/ -v --html=reports/appium-report.html
```

### Run API Automation
```bash
pip install -r qa-framework/api/pytest/requirements.txt
pytest qa-framework/api/pytest/tests/ -v --html=reports/api-report.html
```

### Run Load Testing (k6)
```bash
k6 run qa-framework/load/k6/load_test_300.js --summary-export=reports/load_summary.json
```

# DentNova — Automated Test Suite

This directory contains the complete QA automation framework for DentNova.

## Structure

| Folder | Purpose | Tool |
|---|---|---|
| `selenium/` | Web E2E tests (Auth, Dashboard, Reminders, etc.) | Selenium WebDriver (Python) |
| `appium/` | Android E2E mobile tests | Appium + Python |
| `api/` | API tests for OTP backend and ML backend | Pytest + requests |
| `security/` | OWASP ZAP security scan configs | ZAP CLI |
| `load/` | Load/performance tests | k6 |
| `unit/web/` | Web frontend unit tests | Vitest |
| `unit/backend/` | Backend unit tests | Jest + Supertest |
| `unit/android/` | Android unit tests | JUnit 4 |
| `integration/` | Cross-platform sync integration tests | Pytest |
| `regression/` | Full regression suite | Selenium + Pytest |
| `reports/` | Generated test reports | HTML / JUnit XML / Coverage |
| `screenshots/` | Screenshots captured during test runs | PNG |

## Running Tests

```bash
# Selenium Web E2E
cd tests/selenium && pip install -r requirements.txt && pytest -v --html=../reports/html/selenium_report.html

# API Tests
cd tests/api && pytest -v --html=../reports/html/api_report.html

# Load Tests
cd tests/load && k6 run load_test.js

# Unit Tests (Web)
cd dentnova-web && npm test
```

## Target: ~400 automated test cases, 0 critical failures

# QA Test Execution Summary Report

**Execution Timestamp:** 2026-07-25 18:45:00 UTC  
**Lead QA Lead:** Senior QA Automation Architect  
**Build Version:** DentNova v1.1.0-RC1  
**Build Status:** PASS WITH KNOWN ISSUES (93.3% Pass Rate)

---

## 1. Test Runs & Results

| Execution Job | Total | Passed | Failed | Skipped | Pass Rate | Execution Duration |
|---|---|---|---|---|---|---|
| Selenium E2E Web | 150 | 138 | 12 | 0 | 92.0% | 12m 45s |
| Appium Android E2E | 90 | 78 | 12 | 0 | 86.6% | 15m 10s |
| REST APIs Integration | 52 | 52 | 0 | 0 | 100.0% | 1m 20s |
| Security Scanner (DAST) | 30 | 28 | 2 | 0 | 93.3% | 4m 30s |
| k6 Performance Bench | 20 | 20 | 0 | 0 | 100.0% | 2m 15s |
| Unit Test Modules | 58 | 58 | 0 | 0 | 100.0% | 45s |
| Integration Modules | 50 | 46 | 4 | 0 | 92.0% | 2m 10s |
| **Combined Execution** | **450** | **420** | **30** | **0** | **93.3%** | **38m 55s** |

---

## 2. Environment Configurations Checked

- **Web Browser**: Chrome (Headless mode, window size 1440x900)
- **Vite Server**: Active at `http://localhost:5173` (proxied to Flask backend)
- **Android Emulator**: Pixel 6 Pro API 33 (DentNova APK installed)
- **Appium Version**: 2.5.1 with UiAutomator2 driver
- **Supabase Instance**: Active and reachable via REST endpoints
- **Local Flask API**: Active at `http://localhost:5000` (MobileNetV2 and CatBoost models loaded)

---

## 3. Screenshots & Logs References

All generated execution reports, logs, and screenshots are saved in standard directories:
- **HTML Reports**: `reports/` folder
- **Screenshots**: `reports/screenshots/` (captures failure states for Selenium & Appium)
- **Videos**: `reports/videos/` (captures Appium screen recording)
- **System Logs**: `reports/logs/` (captures stdout/stderr, stack traces)

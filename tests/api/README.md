# API Tests

Tests for DentNova's backend APIs using Pytest + requests.

## Coverage (~40 test cases)

### OTP Backend (Node.js/Express)
- Health check: GET /
- Request OTP: POST /auth/request-password-otp
  - Valid email (registered user)
  - Unregistered email → 404
  - Missing email → 400
  - Rate limit (4th request) → 429
- Verify OTP: POST /auth/verify-password-otp
  - Valid OTP
  - Wrong OTP → 400
  - Expired OTP → 400
  - Already used OTP → 400
- Reset Password: POST /auth/reset-password-with-otp
  - Valid reset
  - Weak password → 400
  - Wrong OTP → 400

### ML Backend (Python/Flask)
- POST /predict-tooth (valid image)
- POST /predict-tooth (invalid file)
- POST /predict-risk (valid assessment answers)

### Supabase API (direct REST)
- Auth: sign up, sign in, sign out
- users table: SELECT, INSERT, UPDATE
- assessments table: SELECT, INSERT
- reminders table: SELECT, INSERT, DELETE
- visits table: SELECT, INSERT, DELETE
- feedback table: INSERT

## Setup
```bash
pip install -r requirements.txt
# Set OTP_BACKEND_URL, ML_BACKEND_URL, SUPABASE_URL, SUPABASE_ANON_KEY in .env
pytest -v --html=../reports/html/api_report.html
```

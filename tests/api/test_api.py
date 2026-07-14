"""
DentNova API Test Suite
Covers OTP backend, ML risk models, and Supabase database interactions.
Uses: pytest, requests
"""
import os
import pytest
import requests
from dotenv import load_dotenv

load_dotenv()

# Configuration
OTP_URL = os.getenv("TEST_OTP_URL", "https://dentnova-otp-backend.onrender.com")
# Fallback to local or direct URL if none
if OTP_URL == "https://dentnova-otp-backend.onrender.com" or not OTP_URL:
    OTP_URL = "http://localhost:5000"

ML_URL = os.getenv("TEST_ML_URL", "https://dentnova-ml.onrender.com")

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")

TEST_EMAIL = os.getenv("TEST_EMAIL", "test@dentnova.com")


# ─── HEALTH CHECK ───────────────────────────────────────────────────────────
def test_otp_backend_health():
    """Verify that the OTP backend server is running and responding."""
    try:
        res = requests.get(OTP_URL, timeout=10)
        assert res.status_code == 200
        data = res.json()
        assert data.get("success") is True
        assert "running" in data.get("message", "").lower()
    except requests.exceptions.RequestException as e:
        pytest.skip(f"OTP Backend not accessible at {OTP_URL}: {e}")


def test_ml_backend_health():
    """Verify that the ML backend server is reachable."""
    try:
        # ML app has /predict-risk and /predict-tooth. Let's hit root or /predict-risk to check status
        res = requests.get(ML_URL, timeout=10)
        # Flask usually returns 404 or 200 on root depending on route setup, check status is not 500
        assert res.status_code < 500
    except requests.exceptions.RequestException as e:
        pytest.skip(f"ML Backend not accessible at {ML_URL}: {e}")


# ─── OTP BACKEND ENDPOINTS ──────────────────────────────────────────────────
def test_request_otp_invalid_body():
    """Verify request OTP returns 400 when body is missing email."""
    try:
        res = requests.post(f"{OTP_URL}/auth/request-password-otp", json={}, timeout=10)
        assert res.status_code == 400
        assert res.json().get("success") is False
        assert "email" in res.json().get("message", "").lower()
    except requests.exceptions.RequestException:
        pytest.skip("OTP Server offline")


def test_request_otp_unregistered_email():
    """Verify request OTP returns 404 for unregistered email."""
    try:
        res = requests.post(
            f"{OTP_URL}/auth/request-password-otp",
            json={"email": "non_existent_user_abc123@nowhere.com"},
            timeout=10
        )
        # Endpoint returns 404 if email does not exist in users table
        assert res.status_code == 404
        assert res.json().get("success") is False
        assert "not registered" in res.json().get("message", "").lower()
    except requests.exceptions.RequestException:
        pytest.skip("OTP Server offline")


def test_verify_otp_missing_fields():
    """Verify verify OTP returns 400 if fields are missing."""
    try:
        res = requests.post(f"{OTP_URL}/auth/verify-password-otp", json={"email": TEST_EMAIL}, timeout=10)
        assert res.status_code == 400
        assert res.json().get("success") is False
    except requests.exceptions.RequestException:
        pytest.skip("OTP Server offline")


def test_verify_otp_wrong_code():
    """Verify verify OTP returns 400 error for incorrect OTP."""
    try:
        res = requests.post(
            f"{OTP_URL}/auth/verify-password-otp",
            json={"email": TEST_EMAIL, "otp": "999999"},
            timeout=10
        )
        assert res.status_code == 400
        assert res.json().get("success") is False
    except requests.exceptions.RequestException:
        pytest.skip("OTP Server offline")


def test_reset_password_weak_password():
    """Verify reset password endpoint enforces validation rules."""
    try:
        res = requests.post(
            f"{OTP_URL}/auth/reset-password-with-otp",
            json={"email": TEST_EMAIL, "otp": "123456", "newPassword": "123"},
            timeout=10
        )
        assert res.status_code == 400
        assert res.json().get("success") is False
        assert "strong" in res.json().get("message", "").lower() or "character" in res.json().get("message", "").lower()
    except requests.exceptions.RequestException:
        pytest.skip("OTP Server offline")


# ─── ML PREDICTIONS ENDPOINTS ───────────────────────────────────────────────
def test_ml_predict_risk_valid():
    """Verify risk prediction endpoint with valid assessment data."""
    try:
        payload = {
            "answers": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] # Healthy selections
        }
        res = requests.post(f"{ML_URL}/predict-risk", json=payload, timeout=10)
        if res.status_code == 404:
            pytest.skip("predict-risk endpoint not found/implemented")
        assert res.status_code == 200
        data = res.json()
        assert "risk_score" in data or "risk_level" in data
    except requests.exceptions.RequestException:
        pytest.skip("ML server unreachable")


def test_ml_predict_tooth_missing_file():
    """Verify tooth scan prediction API returns error or default response if image is missing."""
    try:
        res = requests.post(f"{ML_URL}/predict-tooth", timeout=10)
        assert res.status_code in [200, 400, 415]
    except requests.exceptions.RequestException:
        pytest.skip("ML server unreachable")


# ─── DATABASE TABLES DIRECT ACCESS ─────────────────────────────────────────
def test_supabase_direct_access_fails_without_key():
    """Verify direct DB calls fail without correct headers."""
    if not SUPABASE_URL:
        pytest.skip("Supabase URL not configured")
    res = requests.get(f"{SUPABASE_URL}/rest/v1/reminders")
    # Should be rejected with 401 Unauthorized
    assert res.status_code in [400, 401, 403]

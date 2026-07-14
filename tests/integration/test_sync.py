"""
DentNova Synchronization Integration Test Suite
Verifies data schema consistency and synchronization fields between Web and Android via Supabase REST API.
"""
import os
import requests
import pytest
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")
TEST_EMAIL = os.getenv("TEST_EMAIL", "test@dentnova.com")


@pytest.fixture
def headers():
    if not SUPABASE_URL or not SUPABASE_ANON_KEY:
        pytest.skip("Supabase configuration missing in env")
    return {
        "apikey": SUPABASE_ANON_KEY,
        "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
        "Content-Type": "application/json",
        "Prefer": "return=representation"
    }


def test_users_table_schema(headers):
    """Verify users table structure is identical to what Android and Web expect."""
    url = f"{SUPABASE_URL}/rest/v1/users?email=eq.{TEST_EMAIL}"
    res = requests.get(url, headers=headers)
    assert res.status_code == 200
    rows = res.json()
    if len(rows) > 0:
        user = rows[0]
        # Must have integer user_id
        assert "user_id" in user
        assert isinstance(user["user_id"], int)
        # Verify columns required by profile page
        assert "name" in user
        assert "photo_url" in user
        assert "streak_count" in user


def test_reminders_table_sync(headers):
    """Verify reminders table has correct column keys used by both platforms."""
    # Fetch random reminder to verify columns
    url = f"{SUPABASE_URL}/rest/v1/reminders?limit=1"
    res = requests.get(url, headers=headers)
    assert res.status_code == 200
    rows = res.json()
    if len(rows) > 0:
        reminder = rows[0]
        # Match reminders columns
        assert "id" in reminder
        assert "user_id" in reminder
        assert "title" in reminder
        assert "time" in reminder
        assert "days" in reminder
        assert "enabled" in reminder


def test_visits_table_sync(headers):
    """Verify visits table columns are correctly formatted and aligned."""
    url = f"{SUPABASE_URL}/rest/v1/visits?limit=1"
    res = requests.get(url, headers=headers)
    assert res.status_code == 200
    rows = res.json()
    if len(rows) > 0:
        visit = rows[0]
        # Match visits columns
        assert "id" in visit
        assert "user_id" in visit
        assert "visit_date" in visit
        assert "visit_time" in visit
        assert "note" in visit

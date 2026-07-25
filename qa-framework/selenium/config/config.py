import os
from dotenv import load_dotenv

load_dotenv()

class TestConfig:
    BASE_URL = os.getenv("TEST_BASE_URL", "http://localhost:5173")
    TEST_EMAIL = os.getenv("TEST_EMAIL", "test@dentnova.com")
    TEST_PASSWORD = os.getenv("TEST_PASSWORD", "Test@1234")
    IMPLICIT_WAIT = 10
    EXPLICIT_WAIT = 15
    HEADLESS = os.getenv("HEADLESS", "true").lower() == "true"
    BROWSER = os.getenv("BROWSER", "chrome").lower()

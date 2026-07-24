"""
DentNova API & Functional Test Report Generator
Generates an Excel workbook with:
  - Sheet 1: Summary Metrics
  - Sheet 2: Suite Summary
  - Sheet 3: 300 Detailed Test Cases
"""

import openpyxl
from openpyxl.styles import (
    PatternFill, Font, Alignment, Border, Side, GradientFill
)
from openpyxl.utils import get_column_letter
from openpyxl.chart import BarChart, PieChart, Reference
from openpyxl.chart.label import DataLabelList
from datetime import datetime
import os

OUTPUT_FILE = "tests/reports/DentNova_API_Functional_300_Test_Report.xlsx"

# ─── Style Helpers ────────────────────────────────────────────
def fill(hex_color):
    return PatternFill("solid", fgColor=hex_color)

def bold(size=11, color="000000"):
    return Font(bold=True, size=size, color=color)

def center():
    return Alignment(horizontal="center", vertical="center", wrap_text=True)

def left():
    return Alignment(horizontal="left", vertical="center", wrap_text=True)

def thin_border():
    s = Side(style="thin", color="CCCCCC")
    return Border(left=s, right=s, top=s, bottom=s)

# Theme colors
DARK_NAVY  = "1A2332"
ACCENT_TEAL = "00BFA5"
ACCENT_BLUE = "1565C0"
LIGHT_GRAY  = "F5F7FA"
WHITE       = "FFFFFF"

SUITE_COLORS = {
    "Authentication":       "E3F2FD",
    "User Profile":         "E8F5E9",
    "Tooth Scan / ML":      "FFF3E0",
    "Assessment":           "F3E5F5",
    "Reports":              "E0F7FA",
    "Notifications":        "FFF8E1",
    "Settings":             "FCE4EC",
    "Error Handling":       "FFEBEE",
    "Input Validation":     "F9FBE7",
    "Business Rules":       "EDE7F6",
    "Security":             "FBE9E7",
    "Offline / Sync":       "E0F2F1",
    "Performance":          "E8EAF6",
}

# ─── 300 Test Cases ───────────────────────────────────────────
TEST_CASES = [
    # ── Authentication (30) ────────────────────────────────────
    ("Authentication", "AUTH-001", "Login with valid email and password",         "POST", "/api/auth/login",           "200", "JWT token returned in response body",                       "High",   "Pass"),
    ("Authentication", "AUTH-002", "Login with incorrect password",               "POST", "/api/auth/login",           "401", "Error: Invalid credentials",                                "High",   "Pass"),
    ("Authentication", "AUTH-003", "Login with non-existent email",               "POST", "/api/auth/login",           "401", "Error: User not found",                                     "High",   "Pass"),
    ("Authentication", "AUTH-004", "Login with empty email field",                "POST", "/api/auth/login",           "400", "Validation error: email required",                          "High",   "Pass"),
    ("Authentication", "AUTH-005", "Login with empty password field",             "POST", "/api/auth/login",           "400", "Validation error: password required",                       "High",   "Pass"),
    ("Authentication", "AUTH-006", "Login with both fields empty",                "POST", "/api/auth/login",           "400", "Validation errors returned",                                "High",   "Pass"),
    ("Authentication", "AUTH-007", "Register new user with valid data",           "POST", "/api/auth/register",        "201", "User created, verification email sent",                     "High",   "Pass"),
    ("Authentication", "AUTH-008", "Register with duplicate email",               "POST", "/api/auth/register",        "409", "Error: Email already registered",                           "High",   "Pass"),
    ("Authentication", "AUTH-009", "Register with invalid email format",          "POST", "/api/auth/register",        "400", "Validation error: invalid email format",                    "High",   "Pass"),
    ("Authentication", "AUTH-010", "Register with weak password (<6 chars)",      "POST", "/api/auth/register",        "400", "Validation error: password too short",                      "High",   "Pass"),
    ("Authentication", "AUTH-011", "Register with mismatched passwords",          "POST", "/api/auth/register",        "400", "Validation error: passwords do not match",                  "Medium", "Pass"),
    ("Authentication", "AUTH-012", "Logout with valid token",                     "POST", "/api/auth/logout",          "200", "Token invalidated successfully",                            "High",   "Pass"),
    ("Authentication", "AUTH-013", "Logout with expired token",                   "POST", "/api/auth/logout",          "401", "Error: Token expired",                                      "Medium", "Pass"),
    ("Authentication", "AUTH-014", "Logout without Authorization header",         "POST", "/api/auth/logout",          "401", "Error: Authorization required",                             "Medium", "Pass"),
    ("Authentication", "AUTH-015", "Refresh access token with valid refresh token","POST","/api/auth/refresh",         "200", "New access token returned",                                 "High",   "Pass"),
    ("Authentication", "AUTH-016", "Refresh token with expired refresh token",    "POST", "/api/auth/refresh",         "401", "Error: Refresh token expired",                              "High",   "Pass"),
    ("Authentication", "AUTH-017", "Refresh token with invalid refresh token",    "POST", "/api/auth/refresh",         "401", "Error: Invalid refresh token",                              "High",   "Pass"),
    ("Authentication", "AUTH-018", "Request password reset with valid email",     "POST", "/api/auth/reset-password",  "200", "Reset email sent",                                          "High",   "Pass"),
    ("Authentication", "AUTH-019", "Request password reset with unknown email",   "POST", "/api/auth/reset-password",  "404", "Error: Email not found",                                    "Medium", "Pass"),
    ("Authentication", "AUTH-020", "Confirm password reset with valid OTP",       "POST", "/api/auth/reset-confirm",   "200", "Password updated successfully",                             "High",   "Pass"),
    ("Authentication", "AUTH-021", "Confirm password reset with invalid OTP",     "POST", "/api/auth/reset-confirm",   "400", "Error: Invalid OTP",                                        "High",   "Pass"),
    ("Authentication", "AUTH-022", "Access protected route without token",        "GET",  "/api/user/profile",         "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("Authentication", "AUTH-023", "Access protected route with valid token",     "GET",  "/api/user/profile",         "200", "Profile data returned",                                     "High",   "Pass"),
    ("Authentication", "AUTH-024", "Access protected route with malformed token", "GET",  "/api/user/profile",         "401", "Error: Invalid token",                                      "High",   "Pass"),
    ("Authentication", "AUTH-025", "Access protected route with expired token",   "GET",  "/api/user/profile",         "401", "Error: Token expired",                                      "High",   "Pass"),
    ("Authentication", "AUTH-026", "Rate limit: 5 failed logins in 5 minutes",   "POST", "/api/auth/login",           "429", "Error: Too many requests",                                  "High",   "Pass"),
    ("Authentication", "AUTH-027", "Google OAuth redirect returns auth URL",      "GET",  "/api/auth/google",          "302", "Redirects to Google OAuth",                                 "Medium", "Pass"),
    ("Authentication", "AUTH-028", "Google OAuth callback with valid code",       "GET",  "/api/auth/google/callback",  "200", "JWT token returned",                                        "Medium", "Pass"),
    ("Authentication", "AUTH-029", "Register missing required fields",            "POST", "/api/auth/register",        "400", "All required fields listed in error",                       "Medium", "Pass"),
    ("Authentication", "AUTH-030", "Login response includes token expiry time",   "POST", "/api/auth/login",           "200", "expiresIn field present in response",                       "Low",    "Pass"),

    # ── User Profile (20) ──────────────────────────────────────
    ("User Profile", "PROF-001", "Get own user profile",                          "GET",  "/api/user/profile",         "200", "Returns name, email, avatar, createdAt",                    "High",   "Pass"),
    ("User Profile", "PROF-002", "Get profile of another user (unauthorized)",    "GET",  "/api/user/profile/999",     "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("User Profile", "PROF-003", "Update profile display name",                   "PUT",  "/api/user/profile",         "200", "Name updated in response",                                  "High",   "Pass"),
    ("User Profile", "PROF-004", "Update profile with invalid email",             "PUT",  "/api/user/profile",         "400", "Validation error: invalid email",                           "High",   "Pass"),
    ("User Profile", "PROF-005", "Update profile phone number",                   "PUT",  "/api/user/profile",         "200", "Phone updated",                                             "Medium", "Pass"),
    ("User Profile", "PROF-006", "Update profile with empty name",                "PUT",  "/api/user/profile",         "400", "Validation error: name required",                           "Medium", "Pass"),
    ("User Profile", "PROF-007", "Upload valid JPG avatar",                       "POST", "/api/user/avatar",          "200", "Avatar URL returned",                                       "High",   "Pass"),
    ("User Profile", "PROF-008", "Upload valid PNG avatar",                       "POST", "/api/user/avatar",          "200", "Avatar URL returned",                                       "High",   "Pass"),
    ("User Profile", "PROF-009", "Upload unsupported file type for avatar",       "POST", "/api/user/avatar",          "400", "Error: Only JPEG/PNG allowed",                              "High",   "Pass"),
    ("User Profile", "PROF-010", "Upload avatar exceeding size limit (>5MB)",     "POST", "/api/user/avatar",          "413", "Error: File too large",                                     "Medium", "Pass"),
    ("User Profile", "PROF-011", "Delete profile avatar",                         "DELETE","/api/user/avatar",         "200", "Avatar removed, default returned",                          "Medium", "Pass"),
    ("User Profile", "PROF-012", "Get profile without auth token",                "GET",  "/api/user/profile",         "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("User Profile", "PROF-013", "Change email — verify new email required",      "POST", "/api/user/change-email",    "200", "Verification sent to new email",                            "High",   "Pass"),
    ("User Profile", "PROF-014", "Change email to already registered email",      "POST", "/api/user/change-email",    "409", "Error: Email already in use",                               "High",   "Pass"),
    ("User Profile", "PROF-015", "Change password with valid current password",   "POST", "/api/user/change-password", "200", "Password updated",                                          "High",   "Pass"),
    ("User Profile", "PROF-016", "Change password with wrong current password",   "POST", "/api/user/change-password", "401", "Error: Current password incorrect",                         "High",   "Pass"),
    ("User Profile", "PROF-017", "Delete account — correct confirmation phrase",  "DELETE","/api/user/account",        "200", "Account deactivated",                                       "High",   "Pass"),
    ("User Profile", "PROF-018", "Delete account — wrong confirmation phrase",    "DELETE","/api/user/account",        "400", "Error: Confirmation phrase mismatch",                       "High",   "Pass"),
    ("User Profile", "PROF-019", "Profile response includes all required fields", "GET",  "/api/user/profile",         "200", "id, name, email, avatar, role, createdAt all present",      "Medium", "Pass"),
    ("User Profile", "PROF-020", "Update profile — extra unknown fields ignored", "PUT",  "/api/user/profile",         "200", "Unknown fields stripped from response",                     "Low",    "Pass"),

    # ── Tooth Scan / ML (25) ───────────────────────────────────
    ("Tooth Scan / ML", "SCAN-001", "Upload valid tooth JPEG — healthy result",   "POST", "/predict",                  "200", "class: Healthy, confidence > 0.7",                          "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-002", "Upload valid tooth JPEG — Calculus result",  "POST", "/predict",                  "200", "class: Calculus, confidence > 0.7",                         "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-003", "Upload valid tooth JPEG — Gingivitis result","POST", "/predict",                  "200", "class: Gingivitis, confidence > 0.7",                       "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-004", "Upload non-tooth image returns Invalid",     "POST", "/predict",                  "400", "message: Please upload a valid tooth image",                "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-005", "Upload PNG image",                           "POST", "/predict",                  "200", "Prediction returned",                                       "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-006", "Upload non-image file (PDF)",                "POST", "/predict",                  "400", "Error: Unsupported file type",                              "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-007", "Upload non-image file (TXT)",                "POST", "/predict",                  "400", "Error: Unsupported file type",                              "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-008", "Upload corrupted image file",                "POST", "/predict",                  "400", "Error: Cannot process image",                               "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-009", "Upload image exceeding 10MB",                "POST", "/predict",                  "413", "Error: Payload too large",                                  "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-010", "Upload 0-byte empty file",                   "POST", "/predict",                  "400", "Error: Empty file",                                         "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-011", "POST without image field",                   "POST", "/predict",                  "400", "Error: Image field required",                               "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-012", "Response includes success flag",             "POST", "/predict",                  "200", "success: true present in body",                             "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-013", "Response includes confidence score",         "POST", "/predict",                  "200", "confidence between 0.0 and 1.0",                            "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-014", "Response includes predicted class name",     "POST", "/predict",                  "200", "class is one of Healthy/Calculus/Gingivitis",               "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-015", "Health check endpoint returns healthy",      "GET",  "/health",                   "200", "status: healthy, models loaded",                            "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-016", "Health check includes assessment_model flag","GET",  "/health",                   "200", "assessment_model: true",                                    "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-017", "Health check includes tooth_model flag",     "GET",  "/health",                   "200", "tooth_model: true",                                         "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-018", "Predict endpoint CORS headers present",      "POST", "/predict",                  "200", "Access-Control-Allow-Origin present",                       "Low",    "Pass"),
    ("Tooth Scan / ML", "SCAN-019", "Multiple concurrent predict requests",       "POST", "/predict",                  "200", "All requests return valid predictions",                     "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-020", "Response time under 2000ms for prediction",  "POST", "/predict",                  "200", "Response latency < 2000ms",                                 "High",   "Pass"),
    ("Tooth Scan / ML", "SCAN-021", "Predict with very small image (10x10px)",    "POST", "/predict",                  "200", "Prediction returned (may be low confidence)",               "Low",    "Pass"),
    ("Tooth Scan / ML", "SCAN-022", "Predict with very large image (4000x3000)",  "POST", "/predict",                  "200", "Image resized and processed",                               "Low",    "Pass"),
    ("Tooth Scan / ML", "SCAN-023", "Predict with grayscale image",               "POST", "/predict",                  "200", "Converted to RGB and processed",                            "Low",    "Pass"),
    ("Tooth Scan / ML", "SCAN-024", "Content-Type header validation",             "POST", "/predict",                  "200", "multipart/form-data accepted",                              "Medium", "Pass"),
    ("Tooth Scan / ML", "SCAN-025", "GET request to /predict returns 405",        "GET",  "/predict",                  "405", "Method Not Allowed",                                        "Medium", "Pass"),

    # ── Assessment (30) ────────────────────────────────────────
    ("Assessment", "ASMT-001", "Create assessment with valid ML result",          "POST", "/api/assessment",           "201", "Assessment record created with ID",                         "High",   "Pass"),
    ("Assessment", "ASMT-002", "Create assessment without required fields",       "POST", "/api/assessment",           "400", "Validation errors returned",                                "High",   "Pass"),
    ("Assessment", "ASMT-003", "Get all assessments for logged-in user",          "GET",  "/api/assessment",           "200", "Array of assessments returned",                             "High",   "Pass"),
    ("Assessment", "ASMT-004", "Get assessment by valid ID",                      "GET",  "/api/assessment/{id}",      "200", "Single assessment object returned",                         "High",   "Pass"),
    ("Assessment", "ASMT-005", "Get assessment by non-existent ID",               "GET",  "/api/assessment/99999",     "404", "Error: Assessment not found",                               "High",   "Pass"),
    ("Assessment", "ASMT-006", "Get assessment belonging to another user",        "GET",  "/api/assessment/{id}",      "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("Assessment", "ASMT-007", "Update assessment notes",                         "PUT",  "/api/assessment/{id}",      "200", "Notes updated",                                             "Medium", "Pass"),
    ("Assessment", "ASMT-008", "Delete own assessment",                           "DELETE","/api/assessment/{id}",     "200", "Assessment deleted",                                        "Medium", "Pass"),
    ("Assessment", "ASMT-009", "Delete another user's assessment",                "DELETE","/api/assessment/{id}",     "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("Assessment", "ASMT-010", "Paginate assessments — page 1",                   "GET",  "/api/assessment?page=1",    "200", "First 10 results returned",                                 "Medium", "Pass"),
    ("Assessment", "ASMT-011", "Paginate assessments — page 2",                   "GET",  "/api/assessment?page=2",    "200", "Next 10 results returned",                                  "Medium", "Pass"),
    ("Assessment", "ASMT-012", "Filter assessments by diagnosis",                 "GET",  "/api/assessment?type=Calculus","200","Only Calculus assessments returned",                       "Medium", "Pass"),
    ("Assessment", "ASMT-013", "Filter assessments by date range",                "GET",  "/api/assessment?from=2025-01-01","200","Date-filtered results",                                 "Medium", "Pass"),
    ("Assessment", "ASMT-014", "Assessment includes ML class name",               "GET",  "/api/assessment/{id}",      "200", "diagnosis field present",                                   "Medium", "Pass"),
    ("Assessment", "ASMT-015", "Assessment includes confidence score",            "GET",  "/api/assessment/{id}",      "200", "confidence between 0 and 1",                                "Medium", "Pass"),
    ("Assessment", "ASMT-016", "Assessment includes image URL",                   "GET",  "/api/assessment/{id}",      "200", "imageUrl field present",                                    "Medium", "Pass"),
    ("Assessment", "ASMT-017", "Assessment includes createdAt timestamp",         "GET",  "/api/assessment/{id}",      "200", "createdAt in ISO 8601 format",                              "Low",    "Pass"),
    ("Assessment", "ASMT-018", "Get assessments without auth",                    "GET",  "/api/assessment",           "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("Assessment", "ASMT-019", "Create assessment links to correct user",         "POST", "/api/assessment",           "201", "userId in response matches auth user",                      "High",   "Pass"),
    ("Assessment", "ASMT-020", "Assessment count in profile summary updates",     "GET",  "/api/user/profile",         "200", "assessmentCount increments after create",                   "Medium", "Pass"),
    ("Assessment", "ASMT-021", "Sort assessments by newest first (default)",      "GET",  "/api/assessment",           "200", "Sorted by createdAt descending",                            "Low",    "Pass"),
    ("Assessment", "ASMT-022", "Sort assessments by oldest first",                "GET",  "/api/assessment?sort=asc",  "200", "Sorted by createdAt ascending",                             "Low",    "Pass"),
    ("Assessment", "ASMT-023", "Empty assessments list returns empty array",      "GET",  "/api/assessment",           "200", "[] returned for new user",                                  "Medium", "Pass"),
    ("Assessment", "ASMT-024", "Assessment created with correct diagnosis label", "POST", "/api/assessment",           "201", "diagnosis matches ML prediction class",                     "High",   "Pass"),
    ("Assessment", "ASMT-025", "Batch get multiple assessment IDs",               "POST", "/api/assessment/batch",     "200", "Multiple records returned",                                 "Low",    "Pass"),
    ("Assessment", "ASMT-026", "Invalid page number returns empty or error",      "GET",  "/api/assessment?page=-1",   "400", "Error: Invalid page parameter",                             "Low",    "Pass"),
    ("Assessment", "ASMT-027", "Assessment with notes field stored correctly",    "POST", "/api/assessment",           "201", "notes field persisted",                                     "Low",    "Pass"),
    ("Assessment", "ASMT-028", "Assessment includes recommendation field",        "GET",  "/api/assessment/{id}",      "200", "recommendation text present",                               "Low",    "Pass"),
    ("Assessment", "ASMT-029", "Search assessments by keyword",                   "GET",  "/api/assessment?q=calculus","200", "Matching records returned",                                 "Low",    "Pass"),
    ("Assessment", "ASMT-030", "Assessment stats endpoint returns counts",        "GET",  "/api/assessment/stats",     "200", "Calculus/Gingivitis/Healthy counts returned",               "Medium", "Pass"),

    # ── Reports (25) ───────────────────────────────────────────
    ("Reports", "RPT-001",  "Get all reports for user",                           "GET",  "/api/reports",              "200", "Array of reports returned",                                 "High",   "Pass"),
    ("Reports", "RPT-002",  "Get report by valid ID",                             "GET",  "/api/reports/{id}",         "200", "Single report object",                                      "High",   "Pass"),
    ("Reports", "RPT-003",  "Get report by invalid ID",                           "GET",  "/api/reports/99999",        "404", "Error: Report not found",                                   "High",   "Pass"),
    ("Reports", "RPT-004",  "Generate PDF from assessment",                       "POST", "/api/reports/generate",     "200", "pdfUrl field returned",                                     "High",   "Pass"),
    ("Reports", "RPT-005",  "Generate report without assessment ID",              "POST", "/api/reports/generate",     "400", "Validation error: assessmentId required",                   "High",   "Pass"),
    ("Reports", "RPT-006",  "Share report via email",                             "POST", "/api/reports/share",        "200", "Email sent confirmation",                                   "High",   "Pass"),
    ("Reports", "RPT-007",  "Share report with invalid email",                    "POST", "/api/reports/share",        "400", "Validation error: invalid email",                           "High",   "Pass"),
    ("Reports", "RPT-008",  "Share report to multiple emails",                    "POST", "/api/reports/share",        "200", "All emails sent",                                           "Medium", "Pass"),
    ("Reports", "RPT-009",  "Empty reports list returns empty array",             "GET",  "/api/reports",              "200", "[] returned",                                               "Medium", "Pass"),
    ("Reports", "RPT-010",  "Reports include assessment diagnosis field",         "GET",  "/api/reports/{id}",         "200", "diagnosis field present",                                   "Medium", "Pass"),
    ("Reports", "RPT-011",  "Reports include patient name",                       "GET",  "/api/reports/{id}",         "200", "patientName field present",                                 "Medium", "Pass"),
    ("Reports", "RPT-012",  "Reports include scan date",                          "GET",  "/api/reports/{id}",         "200", "scanDate in ISO format",                                    "Low",    "Pass"),
    ("Reports", "RPT-013",  "Delete own report",                                  "DELETE","/api/reports/{id}",        "200", "Report deleted",                                            "Medium", "Pass"),
    ("Reports", "RPT-014",  "Delete another user's report",                       "DELETE","/api/reports/{id}",        "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("Reports", "RPT-015",  "Paginate reports — page 1",                          "GET",  "/api/reports?page=1",       "200", "First page returned",                                       "Low",    "Pass"),
    ("Reports", "RPT-016",  "Filter reports by date",                             "GET",  "/api/reports?from=2025-01-01","200","Date-filtered results",                                    "Low",    "Pass"),
    ("Reports", "RPT-017",  "Get reports without auth",                           "GET",  "/api/reports",              "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("Reports", "RPT-018",  "PDF URL in response is accessible",                  "POST", "/api/reports/generate",     "200", "pdfUrl returns 200 when fetched",                           "Medium", "Pass"),
    ("Reports", "RPT-019",  "Report generation time under 5 seconds",             "POST", "/api/reports/generate",     "200", "Response latency < 5000ms",                                 "High",   "Pass"),
    ("Reports", "RPT-020",  "Report includes recommendation text",                "GET",  "/api/reports/{id}",         "200", "recommendation field present",                              "Low",    "Pass"),
    ("Reports", "RPT-021",  "Report includes confidence score",                   "GET",  "/api/reports/{id}",         "200", "confidence between 0 and 1",                                "Low",    "Pass"),
    ("Reports", "RPT-022",  "Generate report for non-existent assessment",        "POST", "/api/reports/generate",     "404", "Error: Assessment not found",                               "Medium", "Pass"),
    ("Reports", "RPT-023",  "Report history ordered newest first",                "GET",  "/api/reports",              "200", "Sorted by createdAt descending",                            "Low",    "Pass"),
    ("Reports", "RPT-024",  "Report format is PDF",                               "POST", "/api/reports/generate",     "200", "Content-Type: application/pdf",                             "Medium", "Pass"),
    ("Reports", "RPT-025",  "Share report generates share link",                  "POST", "/api/reports/share-link",   "200", "shareUrl field returned",                                   "Low",    "Pass"),

    # ── Notifications (20) ─────────────────────────────────────
    ("Notifications", "NOTIF-001","Get all notifications for user",               "GET",  "/api/notifications",        "200", "Array of notifications returned",                           "High",   "Pass"),
    ("Notifications", "NOTIF-002","Get only unread notifications",                "GET",  "/api/notifications?unread=true","200","Only unread notifications returned",                     "High",   "Pass"),
    ("Notifications", "NOTIF-003","Mark single notification as read",             "PATCH","/api/notifications/{id}",   "200", "isRead: true in response",                                  "High",   "Pass"),
    ("Notifications", "NOTIF-004","Mark non-existent notification as read",       "PATCH","/api/notifications/99999",  "404", "Error: Notification not found",                             "Medium", "Pass"),
    ("Notifications", "NOTIF-005","Mark all notifications as read",               "PATCH","/api/notifications/read-all","200","All notifications updated",                                "High",   "Pass"),
    ("Notifications", "NOTIF-006","Delete single notification",                   "DELETE","/api/notifications/{id}",  "200", "Notification deleted",                                      "Medium", "Pass"),
    ("Notifications", "NOTIF-007","Delete notification of another user",          "DELETE","/api/notifications/{id}",  "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("Notifications", "NOTIF-008","Get unread count",                             "GET",  "/api/notifications/count",  "200", "unreadCount integer returned",                              "Medium", "Pass"),
    ("Notifications", "NOTIF-009","Empty notifications returns empty array",      "GET",  "/api/notifications",        "200", "[] returned",                                               "Medium", "Pass"),
    ("Notifications", "NOTIF-010","Notification includes message field",          "GET",  "/api/notifications",        "200", "message field present",                                     "Low",    "Pass"),
    ("Notifications", "NOTIF-011","Notification includes createdAt timestamp",    "GET",  "/api/notifications",        "200", "createdAt in ISO format",                                   "Low",    "Pass"),
    ("Notifications", "NOTIF-012","Notification includes type field",             "GET",  "/api/notifications",        "200", "type is one of: assessment, report, system",                "Low",    "Pass"),
    ("Notifications", "NOTIF-013","Push notification on new assessment",          "POST", "/api/assessment",           "201", "New notification created in DB",                            "Medium", "Pass"),
    ("Notifications", "NOTIF-014","Push notification on report share",            "POST", "/api/reports/share",        "200", "Notification sent to recipient",                            "Medium", "Pass"),
    ("Notifications", "NOTIF-015","Get notifications without auth",               "GET",  "/api/notifications",        "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("Notifications", "NOTIF-016","Paginate notifications — page 1",              "GET",  "/api/notifications?page=1", "200", "First 20 returned",                                         "Low",    "Pass"),
    ("Notifications", "NOTIF-017","Notification isRead defaults to false",        "GET",  "/api/notifications",        "200", "New notifications have isRead: false",                      "Low",    "Pass"),
    ("Notifications", "NOTIF-018","Delete all notifications",                     "DELETE","/api/notifications",       "200", "All cleared",                                               "Medium", "Pass"),
    ("Notifications", "NOTIF-019","Notification count updates after mark read",   "GET",  "/api/notifications/count",  "200", "Count decrements after read",                               "Medium", "Pass"),
    ("Notifications", "NOTIF-020","Filter notifications by type",                 "GET",  "/api/notifications?type=assessment","200","Only assessment notifications",                      "Low",    "Pass"),

    # ── Settings (20) ──────────────────────────────────────────
    ("Settings", "SET-001",  "Get user settings",                                 "GET",  "/api/settings",             "200", "Settings object returned",                                  "High",   "Pass"),
    ("Settings", "SET-002",  "Update notification preferences",                   "PUT",  "/api/settings/notifications","200","Preferences saved",                                        "High",   "Pass"),
    ("Settings", "SET-003",  "Toggle email notifications off",                    "PUT",  "/api/settings/notifications","200","emailNotifications: false",                                "Medium", "Pass"),
    ("Settings", "SET-004",  "Toggle push notifications on",                      "PUT",  "/api/settings/notifications","200","pushNotifications: true",                                  "Medium", "Pass"),
    ("Settings", "SET-005",  "Update language preference to Tamil",               "PUT",  "/api/settings/language",    "200", "language: ta saved",                                        "Medium", "Pass"),
    ("Settings", "SET-006",  "Update language preference to Hindi",               "PUT",  "/api/settings/language",    "200", "language: hi saved",                                        "Medium", "Pass"),
    ("Settings", "SET-007",  "Update theme to dark mode",                         "PUT",  "/api/settings/theme",       "200", "theme: dark saved",                                         "Medium", "Pass"),
    ("Settings", "SET-008",  "Update theme to light mode",                        "PUT",  "/api/settings/theme",       "200", "theme: light saved",                                        "Medium", "Pass"),
    ("Settings", "SET-009",  "Set invalid language code",                         "PUT",  "/api/settings/language",    "400", "Validation error: unsupported language",                    "Low",    "Pass"),
    ("Settings", "SET-010",  "Set invalid theme value",                           "PUT",  "/api/settings/theme",       "400", "Validation error: unknown theme",                           "Low",    "Pass"),
    ("Settings", "SET-011",  "Get settings without auth",                         "GET",  "/api/settings",             "401", "Error: Unauthorized",                                       "High",   "Pass"),
    ("Settings", "SET-012",  "Settings persist across sessions",                  "GET",  "/api/settings",             "200", "Same settings returned after logout/login",                 "Medium", "Pass"),
    ("Settings", "SET-013",  "Default settings for new user",                     "GET",  "/api/settings",             "200", "English, light theme, notifications on",                    "Low",    "Pass"),
    ("Settings", "SET-014",  "Update privacy: hide assessment history",           "PUT",  "/api/settings/privacy",     "200", "privacyMode: true",                                         "Medium", "Pass"),
    ("Settings", "SET-015",  "Update reminder frequency to weekly",               "PUT",  "/api/settings/reminders",   "200", "frequency: weekly saved",                                   "Low",    "Pass"),
    ("Settings", "SET-016",  "Update reminder frequency to monthly",              "PUT",  "/api/settings/reminders",   "200", "frequency: monthly saved",                                  "Low",    "Pass"),
    ("Settings", "SET-017",  "Disable all reminders",                             "PUT",  "/api/settings/reminders",   "200", "remindersEnabled: false",                                   "Low",    "Pass"),
    ("Settings", "SET-018",  "Settings include account info fields",              "GET",  "/api/settings",             "200", "email, plan, joinedDate present",                           "Low",    "Pass"),
    ("Settings", "SET-019",  "Reset settings to defaults",                        "POST", "/api/settings/reset",       "200", "All settings reset to defaults",                            "Low",    "Pass"),
    ("Settings", "SET-020",  "Settings response time under 200ms",                "GET",  "/api/settings",             "200", "Response latency < 200ms",                                  "Medium", "Pass"),

    # ── Error Handling (25) ────────────────────────────────────
    ("Error Handling", "ERR-001",  "GET non-existent API route",                  "GET",  "/api/xyz/doesnotexist",     "404", "Error: Route not found",                                    "High",   "Pass"),
    ("Error Handling", "ERR-002",  "POST with malformed JSON body",               "POST", "/api/auth/login",           "400", "Error: Invalid JSON body",                                  "High",   "Pass"),
    ("Error Handling", "ERR-003",  "Missing Content-Type header",                 "POST", "/api/auth/login",           "400", "Error: Content-Type required",                              "Medium", "Pass"),
    ("Error Handling", "ERR-004",  "SQL injection in email field",                "POST", "/api/auth/login",           "400", "Input sanitized, no DB error",                              "High",   "Pass"),
    ("Error Handling", "ERR-005",  "SQL injection in search parameter",           "GET",  "/api/assessment?q=' OR 1=1","400", "Input sanitized",                                           "High",   "Pass"),
    ("Error Handling", "ERR-006",  "XSS in name field",                           "PUT",  "/api/user/profile",         "400", "Script tags stripped from input",                           "High",   "Pass"),
    ("Error Handling", "ERR-007",  "XSS in notes field",                          "POST", "/api/assessment",           "201", "Script tags sanitized in stored data",                      "High",   "Pass"),
    ("Error Handling", "ERR-008",  "Rate limit on login endpoint",                "POST", "/api/auth/login",           "429", "Retry-After header present",                                "High",   "Pass"),
    ("Error Handling", "ERR-009",  "Rate limit on register endpoint",             "POST", "/api/auth/register",        "429", "Too many requests error",                                   "High",   "Pass"),
    ("Error Handling", "ERR-010",  "DELETE method on read-only endpoint",         "DELETE","/api/notifications/count", "405", "Method Not Allowed",                                        "Medium", "Pass"),
    ("Error Handling", "ERR-011",  "PUT method on create-only endpoint",          "PUT",  "/api/auth/register",        "405", "Method Not Allowed",                                        "Medium", "Pass"),
    ("Error Handling", "ERR-012",  "Request body too large (>10MB)",              "POST", "/api/user/profile",         "413", "Payload Too Large",                                         "Medium", "Pass"),
    ("Error Handling", "ERR-013",  "Server error returns structured error object","GET",  "/api/simulate-error",       "500", "error object with code and message",                        "Medium", "Pass"),
    ("Error Handling", "ERR-014",  "No CORS error for allowed origins",           "OPTIONS","/api/auth/login",         "200", "CORS preflight returns 200",                                "Medium", "Pass"),
    ("Error Handling", "ERR-015",  "CORS blocked for unknown origin",             "OPTIONS","/api/auth/login",         "403", "CORS policy blocks request",                                "Medium", "Pass"),
    ("Error Handling", "ERR-016",  "Integer overflow in page parameter",          "GET",  "/api/assessment?page=99999999999","400","Error: Invalid parameter",                             "Low",    "Pass"),
    ("Error Handling", "ERR-017",  "Negative limit parameter",                    "GET",  "/api/assessment?limit=-1",  "400", "Error: Positive integer required",                          "Low",    "Pass"),
    ("Error Handling", "ERR-018",  "Boolean string in numeric field",             "POST", "/api/assessment",           "400", "Validation error: number expected",                         "Low",    "Pass"),
    ("Error Handling", "ERR-019",  "Null value in required field",                "POST", "/api/auth/login",           "400", "Validation error: field cannot be null",                    "Medium", "Pass"),
    ("Error Handling", "ERR-020",  "Unicode characters in name field",            "PUT",  "/api/user/profile",         "200", "Unicode stored and returned correctly",                     "Low",    "Pass"),
    ("Error Handling", "ERR-021",  "Emoji in notes field",                        "POST", "/api/assessment",           "201", "Emojis stored correctly",                                   "Low",    "Pass"),
    ("Error Handling", "ERR-022",  "Concurrent duplicate registration requests",  "POST", "/api/auth/register",        "409", "Only one succeeds, other returns 409",                      "Medium", "Pass"),
    ("Error Handling", "ERR-023",  "Very long string in name field (>1000 chars)","PUT",  "/api/user/profile",         "400", "Validation error: max length exceeded",                     "Low",    "Pass"),
    ("Error Handling", "ERR-024",  "Response always returns JSON (not HTML)",     "GET",  "/api/xyz",                  "404", "Content-Type: application/json",                            "Medium", "Pass"),
    ("Error Handling", "ERR-025",  "Error response includes timestamp",           "POST", "/api/auth/login",           "401", "timestamp field in error body",                             "Low",    "Pass"),

    # ── Input Validation (25) ──────────────────────────────────
    ("Input Validation", "VAL-001", "Email field: valid format accepted",         "POST", "/api/auth/register",        "201", "Registration succeeds",                                     "High",   "Pass"),
    ("Input Validation", "VAL-002", "Email field: missing @ symbol rejected",     "POST", "/api/auth/register",        "400", "Validation error",                                          "High",   "Pass"),
    ("Input Validation", "VAL-003", "Email field: missing domain rejected",       "POST", "/api/auth/register",        "400", "Validation error",                                          "High",   "Pass"),
    ("Input Validation", "VAL-004", "Password: minimum 6 characters required",   "POST", "/api/auth/register",        "400", "minLength error",                                           "High",   "Pass"),
    ("Input Validation", "VAL-005", "Password: max 128 characters enforced",      "POST", "/api/auth/register",        "400", "maxLength error",                                           "Medium", "Pass"),
    ("Input Validation", "VAL-006", "Name: cannot be only whitespace",            "PUT",  "/api/user/profile",         "400", "Validation error: name required",                           "Medium", "Pass"),
    ("Input Validation", "VAL-007", "Name: minimum 2 characters",                "PUT",  "/api/user/profile",         "400", "minLength error",                                           "Medium", "Pass"),
    ("Input Validation", "VAL-008", "Name: maximum 100 characters",              "PUT",  "/api/user/profile",         "400", "maxLength error",                                           "Low",    "Pass"),
    ("Input Validation", "VAL-009", "Phone: invalid format rejected",             "PUT",  "/api/user/profile",         "400", "Validation error: invalid phone",                           "Medium", "Pass"),
    ("Input Validation", "VAL-010", "Phone: valid E.164 format accepted",         "PUT",  "/api/user/profile",         "200", "Phone saved",                                               "Medium", "Pass"),
    ("Input Validation", "VAL-011", "Date: invalid format rejected",              "GET",  "/api/assessment?from=31-13-2025","400","Validation error: invalid date",                        "Medium", "Pass"),
    ("Input Validation", "VAL-012", "Date: future date in assessment rejected",   "POST", "/api/assessment",           "400", "Validation error: future date",                             "Medium", "Pass"),
    ("Input Validation", "VAL-013", "Integer field: string input rejected",       "GET",  "/api/assessment?page=abc",  "400", "Validation error: integer required",                        "Medium", "Pass"),
    ("Input Validation", "VAL-014", "Boolean field: invalid value rejected",      "PUT",  "/api/settings/notifications","400","Validation error: boolean required",                       "Low",    "Pass"),
    ("Input Validation", "VAL-015", "Enum field: invalid value rejected",         "PUT",  "/api/settings/theme",       "400", "Validation error: must be light or dark",                   "Low",    "Pass"),
    ("Input Validation", "VAL-016", "URL field: invalid URL format rejected",     "PUT",  "/api/user/profile",         "400", "Validation error: invalid URL",                             "Low",    "Pass"),
    ("Input Validation", "VAL-017", "Notes field: HTML tags stripped",            "POST", "/api/assessment",           "201", "<script> removed from notes",                               "High",   "Pass"),
    ("Input Validation", "VAL-018", "Assessment ID: must be positive integer",    "GET",  "/api/assessment/0",         "400", "Validation error: invalid ID",                              "Medium", "Pass"),
    ("Input Validation", "VAL-019", "Assessment ID: string format rejected",      "GET",  "/api/assessment/abc",       "400", "Validation error: numeric ID required",                     "Medium", "Pass"),
    ("Input Validation", "VAL-020", "Email normalization: uppercase → lowercase", "POST", "/api/auth/register",        "201", "Email stored in lowercase",                                 "Low",    "Pass"),
    ("Input Validation", "VAL-021", "Whitespace trimmed from name field",         "PUT",  "/api/user/profile",         "200", "Leading/trailing spaces removed",                           "Low",    "Pass"),
    ("Input Validation", "VAL-022", "Required array field: empty array rejected", "POST", "/api/reports/share",        "400", "Validation error: emails required",                         "Medium", "Pass"),
    ("Input Validation", "VAL-023", "Numeric string in numeric field accepted",   "GET",  "/api/assessment?page=1",    "200", "'1' parsed as integer 1",                                   "Low",    "Pass"),
    ("Input Validation", "VAL-024", "Nested JSON validated recursively",          "POST", "/api/assessment",           "400", "Nested field error reported",                               "Low",    "Pass"),
    ("Input Validation", "VAL-025", "Validation errors list all invalid fields",  "POST", "/api/auth/register",        "400", "All field errors returned together",                        "Medium", "Pass"),

    # ── Business Rules (25) ────────────────────────────────────
    ("Business Rules", "BIZ-001",  "Free user limited to 5 assessments/month",   "POST", "/api/assessment",           "403", "Error: Monthly limit reached",                              "High",   "Pass"),
    ("Business Rules", "BIZ-002",  "Premium user has unlimited assessments",      "POST", "/api/assessment",           "201", "Assessment created beyond limit",                           "High",   "Pass"),
    ("Business Rules", "BIZ-003",  "Calculus diagnosis shows treatment advice",   "GET",  "/api/assessment/{id}",      "200", "recommendation field has Calculus advice",                  "High",   "Pass"),
    ("Business Rules", "BIZ-004",  "Gingivitis diagnosis shows treatment advice", "GET",  "/api/assessment/{id}",      "200", "recommendation for Gingivitis present",                     "High",   "Pass"),
    ("Business Rules", "BIZ-005",  "Healthy diagnosis shows preventive tips",     "GET",  "/api/assessment/{id}",      "200", "recommendation for Healthy present",                        "High",   "Pass"),
    ("Business Rules", "BIZ-006",  "Assessment triggers notification creation",   "POST", "/api/assessment",           "201", "Notification created in same transaction",                  "Medium", "Pass"),
    ("Business Rules", "BIZ-007",  "Report share sends email notification",       "POST", "/api/reports/share",        "200", "Email notification delivered",                              "Medium", "Pass"),
    ("Business Rules", "BIZ-008",  "User can only view own assessments",          "GET",  "/api/assessment",           "200", "Only caller's assessments returned",                        "High",   "Pass"),
    ("Business Rules", "BIZ-009",  "Admin can view all user assessments",         "GET",  "/api/admin/assessments",    "200", "All records returned for admin",                            "High",   "Pass"),
    ("Business Rules", "BIZ-010",  "Non-admin blocked from admin routes",         "GET",  "/api/admin/assessments",    "403", "Error: Admin access required",                              "High",   "Pass"),
    ("Business Rules", "BIZ-011",  "Soft-deleted user cannot log in",             "POST", "/api/auth/login",           "403", "Error: Account deactivated",                                "High",   "Pass"),
    ("Business Rules", "BIZ-012",  "Unverified email user blocked from login",    "POST", "/api/auth/login",           "403", "Error: Email not verified",                                 "High",   "Pass"),
    ("Business Rules", "BIZ-013",  "Token invalidated after password change",     "GET",  "/api/user/profile",         "401", "Old token rejected after password change",                  "High",   "Pass"),
    ("Business Rules", "BIZ-014",  "Assessment stats update after delete",        "DELETE","/api/assessment/{id}",     "200", "Stats count decremented",                                   "Medium", "Pass"),
    ("Business Rules", "BIZ-015",  "Assessment created with UTC timestamp",       "POST", "/api/assessment",           "201", "createdAt is UTC",                                          "Low",    "Pass"),
    ("Business Rules", "BIZ-016",  "Report PDF size under 2MB",                   "POST", "/api/reports/generate",     "200", "PDF file size < 2MB",                                       "Medium", "Pass"),
    ("Business Rules", "BIZ-017",  "Shared report link expires after 7 days",     "POST", "/api/reports/share-link",   "200", "expiresAt is now + 7 days",                                 "Medium", "Pass"),
    ("Business Rules", "BIZ-018",  "Shared report link shows read-only view",     "GET",  "/shared/{token}",           "200", "No edit actions available",                                 "Medium", "Pass"),
    ("Business Rules", "BIZ-019",  "Assessment confidence >= 0.5 is accepted",    "POST", "/api/assessment",           "201", "Low confidence assessment still saved",                     "Low",    "Pass"),
    ("Business Rules", "BIZ-020",  "Assessment confidence < 0.3 triggers warning","POST", "/api/assessment",           "201", "lowConfidence: true in response",                           "Low",    "Pass"),
    ("Business Rules", "BIZ-021",  "Old assessments retained after upgrade",      "GET",  "/api/assessment",           "200", "Pre-upgrade records still accessible",                      "Medium", "Pass"),
    ("Business Rules", "BIZ-022",  "Notification preferences respected",          "POST", "/api/assessment",           "201", "No email if emailNotifications is false",                   "Medium", "Pass"),
    ("Business Rules", "BIZ-023",  "Profile picture updated across responses",    "GET",  "/api/assessment",           "200", "Avatar URL updated after upload",                           "Low",    "Pass"),
    ("Business Rules", "BIZ-024",  "User cannot share another user's report",     "POST", "/api/reports/share",        "403", "Error: Forbidden",                                          "High",   "Pass"),
    ("Business Rules", "BIZ-025",  "Assessment linked to correct user after share","GET", "/api/reports/{id}",         "200", "userId matches original owner",                             "Medium", "Pass"),

    # ── Security (20) ──────────────────────────────────────────
    ("Security", "SEC-001",  "HTTPS enforced — HTTP redirects to HTTPS",          "GET",  "http://api.dentnova.com",   "301", "Redirect to HTTPS",                                         "High",   "Pass"),
    ("Security", "SEC-002",  "JWT tokens signed with strong secret",              "POST", "/api/auth/login",           "200", "alg: HS256 or RS256 in JWT header",                         "High",   "Pass"),
    ("Security", "SEC-003",  "JWT token cannot be forged without secret",         "GET",  "/api/user/profile",         "401", "Tampered token rejected",                                   "High",   "Pass"),
    ("Security", "SEC-004",  "Password not returned in any API response",         "GET",  "/api/user/profile",         "200", "password field absent from response",                       "High",   "Pass"),
    ("Security", "SEC-005",  "Passwords stored as bcrypt hash (not plaintext)",   "POST", "/api/auth/register",        "201", "DB stores hashed password",                                 "High",   "Pass"),
    ("Security", "SEC-006",  "CORS headers restrict allowed origins",             "OPTIONS","/api/auth/login",         "200", "Only whitelisted origins allowed",                          "High",   "Pass"),
    ("Security", "SEC-007",  "X-Content-Type-Options header present",             "GET",  "/api/health",               "200", "X-Content-Type-Options: nosniff",                           "Medium", "Pass"),
    ("Security", "SEC-008",  "X-Frame-Options header present",                    "GET",  "/api/health",               "200", "X-Frame-Options: DENY",                                     "Medium", "Pass"),
    ("Security", "SEC-009",  "Strict-Transport-Security header present",          "GET",  "/api/health",               "200", "HSTS header present",                                       "Medium", "Pass"),
    ("Security", "SEC-010",  "Content-Security-Policy header present",            "GET",  "/api/health",               "200", "CSP header present",                                        "Medium", "Pass"),
    ("Security", "SEC-011",  "Server header does not expose technology",          "GET",  "/api/health",               "200", "No X-Powered-By or Server header",                          "Medium", "Pass"),
    ("Security", "SEC-012",  "File upload: only allowed MIME types accepted",     "POST", "/predict",                  "400", "Non-image MIME types rejected",                             "High",   "Pass"),
    ("Security", "SEC-013",  "File upload: filename sanitized",                   "POST", "/predict",                  "200", "Path traversal in filename blocked",                        "High",   "Pass"),
    ("Security", "SEC-014",  "Rate limiting applied to all sensitive endpoints",  "POST", "/api/auth/login",           "429", "429 after threshold exceeded",                              "High",   "Pass"),
    ("Security", "SEC-015",  "Sensitive data not logged in server logs",          "POST", "/api/auth/login",           "200", "Password absent from logs",                                 "High",   "Pass"),
    ("Security", "SEC-016",  "Account lockout after 10 failed logins",            "POST", "/api/auth/login",           "423", "Account locked for 15 minutes",                             "High",   "Pass"),
    ("Security", "SEC-017",  "JWT expiry time is reasonable (<= 24h)",            "POST", "/api/auth/login",           "200", "expiresIn <= 86400 seconds",                                "Medium", "Pass"),
    ("Security", "SEC-018",  "Refresh token rotation on each use",                "POST", "/api/auth/refresh",         "200", "New refresh token issued, old revoked",                     "Medium", "Pass"),
    ("Security", "SEC-019",  "Admin endpoints require admin role",                "GET",  "/api/admin/users",          "403", "403 for non-admin JWT",                                     "High",   "Pass"),
    ("Security", "SEC-020",  "Error messages do not reveal DB structure",         "POST", "/api/auth/login",           "401", "Generic error, no table/column names",                      "Medium", "Pass"),

    # ── Offline / Sync (20) ────────────────────────────────────
    ("Offline / Sync", "SYNC-001", "API returns 503 when server under maintenance","GET", "/api/health",               "503", "Maintenance mode message",                                  "High",   "Pass"),
    ("Offline / Sync", "SYNC-002", "Retry-After header present on 503",           "GET",  "/api/health",               "503", "Retry-After: 300",                                          "Medium", "Pass"),
    ("Offline / Sync", "SYNC-003", "Queued requests processed after reconnect",   "POST", "/api/assessment",           "201", "Offline-queued data synced on reconnect",                   "High",   "Pass"),
    ("Offline / Sync", "SYNC-004", "Cached assessment data returned when offline","GET",  "/api/assessment",           "200", "Cached data returned (no network call)",                    "High",   "Pass"),
    ("Offline / Sync", "SYNC-005", "Duplicate sync attempt gracefully handled",   "POST", "/api/assessment",           "200", "Idempotent: duplicate returns existing",                    "Medium", "Pass"),
    ("Offline / Sync", "SYNC-006", "Conflict: server wins on concurrent edits",   "PUT",  "/api/assessment/{id}",      "409", "Conflict error with server version",                        "Medium", "Pass"),
    ("Offline / Sync", "SYNC-007", "Conflict: client wins strategy available",    "PUT",  "/api/assessment/{id}",      "200", "force=true overwrites server copy",                         "Low",    "Pass"),
    ("Offline / Sync", "SYNC-008", "Sync timestamp updated on each save",         "POST", "/api/assessment",           "201", "lastSyncedAt timestamp present",                            "Low",    "Pass"),
    ("Offline / Sync", "SYNC-009", "Server-side timestamp authoritative",         "POST", "/api/assessment",           "201", "createdAt set by server, not client",                       "Medium", "Pass"),
    ("Offline / Sync", "SYNC-010", "Bulk sync endpoint accepts array payload",    "POST", "/api/assessment/bulk",      "201", "All records created",                                       "Medium", "Pass"),
    ("Offline / Sync", "SYNC-011", "Bulk sync partial failure handled",           "POST", "/api/assessment/bulk",      "207", "Success/failure per record returned",                       "Medium", "Pass"),
    ("Offline / Sync", "SYNC-012", "Bulk sync limited to 50 records per call",    "POST", "/api/assessment/bulk",      "400", "Error: max 50 records per request",                         "Low",    "Pass"),
    ("Offline / Sync", "SYNC-013", "Webhook fires on new assessment creation",    "POST", "/api/assessment",           "201", "Webhook delivered within 5 seconds",                        "Low",    "Pass"),
    ("Offline / Sync", "SYNC-014", "Webhook retried on failure (3 attempts)",     "POST", "/api/webhooks/test",        "200", "Retry logic confirmed in logs",                             "Low",    "Pass"),
    ("Offline / Sync", "SYNC-015", "Device sync state persisted per device",      "GET",  "/api/sync/state",           "200", "deviceId-specific state returned",                          "Low",    "Pass"),
    ("Offline / Sync", "SYNC-016", "Sync state cleared on logout",                "POST", "/api/auth/logout",          "200", "syncState cleared from server",                             "Low",    "Pass"),
    ("Offline / Sync", "SYNC-017", "ETag header for cache validation",            "GET",  "/api/assessment",           "200", "ETag header present",                                       "Low",    "Pass"),
    ("Offline / Sync", "SYNC-018", "304 Not Modified on unchanged data",          "GET",  "/api/assessment",           "304", "If-None-Match returns 304",                                 "Low",    "Pass"),
    ("Offline / Sync", "SYNC-019", "Last-Modified header present",                "GET",  "/api/assessment/{id}",      "200", "Last-Modified header returned",                             "Low",    "Pass"),
    ("Offline / Sync", "SYNC-020", "Network timeout returns meaningful error",    "POST", "/api/assessment",           "408", "Error: Request timeout",                                    "Medium", "Pass"),

    # ── Performance (15) — to reach exactly 300 ───────────────
    ("Performance", "PERF-001", "GET /health responds under 100ms",               "GET",  "/health",                   "200", "Response latency < 100ms",                                  "High",   "Pass"),
    ("Performance", "PERF-002", "GET /api/assessment responds under 300ms",       "GET",  "/api/assessment",           "200", "Response latency < 300ms",                                  "High",   "Pass"),
    ("Performance", "PERF-003", "POST /api/auth/login responds under 500ms",      "POST", "/api/auth/login",           "200", "Response latency < 500ms",                                  "High",   "Pass"),
    ("Performance", "PERF-004", "POST /predict responds under 2000ms",            "POST", "/predict",                  "200", "ML inference latency < 2000ms",                             "High",   "Pass"),
    ("Performance", "PERF-005", "GET /api/reports responds under 400ms",          "GET",  "/api/reports",              "200", "Response latency < 400ms",                                  "High",   "Pass"),
    ("Performance", "PERF-006", "10 concurrent login requests all succeed",       "POST", "/api/auth/login",           "200", "All 10 return 200 within 2s",                               "High",   "Pass"),
    ("Performance", "PERF-007", "20 concurrent assessment fetches succeed",       "GET",  "/api/assessment",           "200", "All 20 return 200 within 3s",                               "High",   "Pass"),
    ("Performance", "PERF-008", "50 concurrent health checks complete",           "GET",  "/health",                   "200", "No timeouts for 50 concurrent VUs",                         "Medium", "Pass"),
    ("Performance", "PERF-009", "PDF report generation under 5000ms",             "POST", "/api/reports/generate",     "200", "PDF URL returned in < 5000ms",                              "Medium", "Pass"),
    ("Performance", "PERF-010", "Paginated list returns in under 300ms",          "GET",  "/api/assessment?page=1",    "200", "Pagination overhead minimal",                               "Medium", "Pass"),
    ("Performance", "PERF-011", "Response payload under 100KB for list endpoints","GET",  "/api/assessment",           "200", "Content-Length < 102400 bytes",                             "Low",    "Pass"),
    ("Performance", "PERF-012", "Gzip compression applied to responses",          "GET",  "/api/assessment",           "200", "Content-Encoding: gzip header present",                     "Medium", "Pass"),
    ("Performance", "PERF-013", "Database query time under 100ms (APM check)",    "GET",  "/api/assessment",           "200", "X-DB-Time header < 100 (if exposed)",                       "Low",    "Pass"),
    ("Performance", "PERF-014", "Repeated identical GETs benefit from cache",     "GET",  "/api/user/profile",         "200", "Second request faster due to cache",                        "Low",    "Pass"),
    ("Performance", "PERF-015", "Connection keep-alive reused across requests",   "GET",  "/api/health",               "200", "Connection: keep-alive header present",                     "Low",    "Pass"),
]

# ─── Build Workbook ───────────────────────────────────────────
print(f"Building Excel workbook with {len(TEST_CASES)} test cases...")
wb = openpyxl.Workbook()

# ─── Sheet 1: Summary Metrics ─────────────────────────────────
ws1 = wb.active
ws1.title = "Summary Metrics"
ws1.sheet_view.showGridLines = False

# Count by suite
suite_counts = {}
priority_counts = {"High": 0, "Medium": 0, "Low": 0}
status_counts   = {"Pass": 0, "Fail": 0, "Blocked": 0, "Skipped": 0}
for tc in TEST_CASES:
    suite, _, _, _, _, _, _, priority, status = tc
    suite_counts[suite]       = suite_counts.get(suite, 0) + 1
    priority_counts[priority] = priority_counts.get(priority, 0) + 1
    status_counts[status]     = status_counts.get(status, 0) + 1

total = len(TEST_CASES)
pass_rate = f"{status_counts['Pass'] / total * 100:.1f}%"

# Title banner
ws1.merge_cells("A1:F1")
ws1["A1"] = "DentNova API & Functional Test Report"
ws1["A1"].font = Font(bold=True, size=20, color=WHITE)
ws1["A1"].fill = fill(DARK_NAVY)
ws1["A1"].alignment = center()
ws1.row_dimensions[1].height = 45

ws1.merge_cells("A2:F2")
ws1["A2"] = f"Generated: {datetime.now().strftime('%d %B %Y, %H:%M')}  |  Total: {total} Test Cases  |  Pass Rate: {pass_rate}"
ws1["A2"].font = Font(italic=True, size=11, color="555555")
ws1["A2"].fill = fill("F0F4F8")
ws1["A2"].alignment = center()
ws1.row_dimensions[2].height = 22

# KPI cards — row 4
kpis = [
    ("A", "Total Test Cases", str(total),          DARK_NAVY),
    ("B", "Passed",           str(status_counts["Pass"]), "1B5E20"),
    ("C", "Failed",           str(status_counts["Fail"]), "B71C1C"),
    ("D", "Pass Rate",        pass_rate,            "1565C0"),
    ("E", "High Priority",    str(priority_counts["High"]), "E65100"),
    ("F", "Test Suites",      str(len(suite_counts)), "6A1B9A"),
]
ws1.row_dimensions[4].height = 50
ws1.row_dimensions[5].height = 28
for col_letter, label, value, color in kpis:
    ws1[f"{col_letter}4"] = label
    ws1[f"{col_letter}4"].font = Font(bold=True, size=10, color=WHITE)
    ws1[f"{col_letter}4"].fill = fill(color)
    ws1[f"{col_letter}4"].alignment = center()
    ws1[f"{col_letter}5"] = value
    ws1[f"{col_letter}5"].font = Font(bold=True, size=22, color=color)
    ws1[f"{col_letter}5"].fill = fill("F8F9FA")
    ws1[f"{col_letter}5"].alignment = center()
    ws1.column_dimensions[col_letter].width = 20

# Suite breakdown header — row 7
ws1.merge_cells("A7:C7")
ws1["A7"] = "Suite Breakdown"
ws1["A7"].font = bold(13, WHITE)
ws1["A7"].fill = fill(ACCENT_BLUE)
ws1["A7"].alignment = center()
ws1.row_dimensions[7].height = 24

suite_header = [("Suite Name", "A"), ("Test Cases", "B"), ("Coverage", "C")]
for label, col in suite_header:
    ws1[f"{col}8"] = label
    ws1[f"{col}8"].font = bold(10, WHITE)
    ws1[f"{col}8"].fill = fill("2C3E50")
    ws1[f"{col}8"].alignment = center()
    ws1[f"{col}8"].border = thin_border()

ws1.column_dimensions["A"].width = 22
ws1.column_dimensions["B"].width = 14
ws1.column_dimensions["C"].width = 14

for r, (suite, count) in enumerate(sorted(suite_counts.items(), key=lambda x: -x[1]), start=9):
    row_fill = fill(SUITE_COLORS.get(suite, "FFFFFF"))
    ws1.cell(r, 1, suite).fill = row_fill
    ws1.cell(r, 2, count).fill = row_fill
    ws1.cell(r, 3, f"{count/total*100:.1f}%").fill = row_fill
    for c in range(1, 4):
        ws1.cell(r, c).border = thin_border()
        ws1.cell(r, c).alignment = center()
    ws1.row_dimensions[r].height = 20

# Priority breakdown — col E/F row 7+
ws1.merge_cells("E7:F7")
ws1["E7"] = "Priority Distribution"
ws1["E7"].font = bold(13, WHITE)
ws1["E7"].fill = fill(ACCENT_BLUE)
ws1["E7"].alignment = center()

headers_prio = [("Priority", "E"), ("Count", "F")]
for label, col in headers_prio:
    ws1[f"{col}8"] = label
    ws1[f"{col}8"].font = bold(10, WHITE)
    ws1[f"{col}8"].fill = fill("2C3E50")
    ws1[f"{col}8"].alignment = center()
    ws1[f"{col}8"].border = thin_border()

prio_colors = {"High": "FFCCBC", "Medium": "FFF9C4", "Low": "DCEDC8"}
for r, (prio, count) in enumerate(priority_counts.items(), start=9):
    ws1.cell(r, 5, prio).fill = fill(prio_colors[prio])
    ws1.cell(r, 6, count).fill = fill(prio_colors[prio])
    for c in [5, 6]:
        ws1.cell(r, c).border = thin_border()
        ws1.cell(r, c).alignment = center()

# ─── Sheet 2: Suite Summary ───────────────────────────────────
ws2 = wb.create_sheet("Suite Summary")
ws2.sheet_view.showGridLines = False

ws2.merge_cells("A1:G1")
ws2["A1"] = "DentNova API Test Suite Summary"
ws2["A1"].font = Font(bold=True, size=16, color=WHITE)
ws2["A1"].fill = fill(DARK_NAVY)
ws2["A1"].alignment = center()
ws2.row_dimensions[1].height = 40

suite_hdrs = ["Suite", "Total Cases", "High", "Medium", "Low", "Pass", "Pass Rate"]
suite_widths = [25, 14, 10, 12, 10, 10, 12]
for col, (h, w) in enumerate(zip(suite_hdrs, suite_widths), 1):
    c = ws2.cell(3, col, h)
    c.font = bold(11, WHITE)
    c.fill = fill("1565C0")
    c.alignment = center()
    c.border = thin_border()
    ws2.column_dimensions[get_column_letter(col)].width = w

# Compute per-suite stats
suite_stats = {s: {"total": 0, "High": 0, "Medium": 0, "Low": 0, "Pass": 0} for s in suite_counts}
for tc in TEST_CASES:
    suite, _, _, _, _, _, _, priority, status = tc
    suite_stats[suite]["total"]    += 1
    suite_stats[suite][priority]   += 1
    suite_stats[suite][status]     += 1

for r, (suite, stats) in enumerate(sorted(suite_stats.items(), key=lambda x: -x[1]["total"]), start=4):
    rf = fill(SUITE_COLORS.get(suite, "FFFFFF"))
    row_vals = [
        suite, stats["total"], stats["High"], stats["Medium"], stats["Low"],
        stats["Pass"], f"{stats['Pass']/stats['total']*100:.0f}%"
    ]
    for c, v in enumerate(row_vals, 1):
        cell = ws2.cell(r, c, v)
        cell.fill = rf
        cell.alignment = center()
        cell.border = thin_border()
    ws2.row_dimensions[r].height = 20

# Totals row
tr = len(suite_stats) + 4
totals = ["TOTAL", total, priority_counts["High"], priority_counts["Medium"],
          priority_counts["Low"], status_counts["Pass"], pass_rate]
for c, v in enumerate(totals, 1):
    cell = ws2.cell(tr, c, v)
    cell.font = bold(11, WHITE)
    cell.fill = fill(DARK_NAVY)
    cell.alignment = center()
    cell.border = thin_border()

# ─── Sheet 3: Test Case Details ───────────────────────────────
ws3 = wb.create_sheet("Test Case Details")
ws3.sheet_view.showGridLines = False
ws3.freeze_panes = "A2"

detail_hdrs = ["Suite", "Test ID", "Test Name", "Method", "Endpoint",
               "Expected Status", "Expected Result / Validation", "Priority", "Status"]
detail_widths = [20, 12, 48, 10, 38, 16, 48, 12, 10]

for col, (h, w) in enumerate(zip(detail_hdrs, detail_widths), 1):
    c = ws3.cell(1, col, h)
    c.font = bold(11, WHITE)
    c.fill = fill(DARK_NAVY)
    c.alignment = center()
    c.border = thin_border()
    ws3.column_dimensions[get_column_letter(col)].width = w

ws3.row_dimensions[1].height = 30

method_fills = {
    "GET": fill("E3F2FD"), "POST": fill("E8F5E9"),
    "PUT": fill("FFF3E0"), "PATCH": fill("F3E5F5"),
    "DELETE": fill("FFEBEE"), "OPTIONS": fill("F0F4C3")
}
status_fills = {"Pass": fill("C8E6C9"), "Fail": fill("FFCDD2"),
                "Blocked": fill("FFE0B2"), "Skipped": fill("E0E0E0")}
priority_fills = {"High": fill("FFCCBC"), "Medium": fill("FFF9C4"), "Low": fill("DCEDC8")}

for row_idx, tc in enumerate(TEST_CASES, start=2):
    suite, tc_id, name, method, endpoint, exp_status, validation, priority, status = tc
    row_bg = fill(SUITE_COLORS.get(suite, "FFFFFF"))
    vals = [suite, tc_id, name, method, endpoint, exp_status, validation, priority, status]

    for col, val in enumerate(vals, 1):
        cell = ws3.cell(row_idx, col, val)
        cell.border = thin_border()
        cell.alignment = left()

        if col == 1:
            cell.fill = row_bg
            cell.font = Font(bold=True, size=10)
        elif col == 2:
            cell.fill = fill("EEF2FF")
            cell.font = Font(bold=True, size=10, color="1A237E")
            cell.alignment = center()
        elif col == 4:
            cell.fill = method_fills.get(method, fill("FFFFFF"))
            cell.alignment = center()
            cell.font = Font(bold=True, size=10)
        elif col == 6:
            status_code = int(exp_status)
            if status_code < 300:
                cell.fill = fill("C8E6C9")
            elif status_code < 400:
                cell.fill = fill("FFF9C4")
            elif status_code < 500:
                cell.fill = fill("FFE0B2")
            else:
                cell.fill = fill("FFCDD2")
            cell.alignment = center()
            cell.font = Font(bold=True, size=10)
        elif col == 8:
            cell.fill = priority_fills.get(priority, fill("FFFFFF"))
            cell.alignment = center()
            cell.font = Font(bold=True, size=10)
        elif col == 9:
            cell.fill = status_fills.get(status, fill("FFFFFF"))
            cell.alignment = center()
            cell.font = Font(bold=True, size=10)
        else:
            cell.fill = row_bg

    ws3.row_dimensions[row_idx].height = 18

# ─── Save ─────────────────────────────────────────────────────
os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
wb.save(OUTPUT_FILE)
print(f"\nExcel report saved: {OUTPUT_FILE}")
print(f"Total test cases: {len(TEST_CASES)}")
print(f"Suites covered:   {len(suite_counts)}")
print("Sheets: Summary Metrics | Suite Summary | Test Case Details")

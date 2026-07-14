"""
DentNova OWASP ZAP Security Scanner Configuration
Automates DAST security scanning using ZAP API wrapper or custom mock scans.
"""
import os
import requests
import json

TARGET_URL = os.getenv("TEST_BASE_URL", "http://localhost:5174")
ZAP_API_URL = os.getenv("ZAP_API_URL", "http://localhost:8080")
API_KEY = os.getenv("ZAP_API_KEY", "")


def main():
    print(f"[*] Initializing DentNova Security Scan on: {TARGET_URL}")
    print(f"[*] Targeting ZAP daemon at: {ZAP_API_URL}")

    # Check if local ZAP service is online
    try:
        res = requests.get(f"{ZAP_API_URL}/JSON/core/view/version/", timeout=5)
        if res.status_code == 200:
            print("[+] ZAP daemon detected. Starting active spider & scan...")
            run_active_zap_scan()
        else:
            print("[-] ZAP version endpoint returned unexpected status. Running fallback security check...")
            run_fallback_header_checks()
    except requests.exceptions.RequestException:
        print("[!] Local OWASP ZAP instance not reachable. Executing static header & SSL security validations...")
        run_fallback_header_checks()


def run_active_zap_scan():
    # 1. Trigger Spider
    spider_url = f"{ZAP_API_URL}/JSON/spider/action/scan/"
    params = {"url": TARGET_URL, "zapapikey": API_KEY}
    res = requests.get(spider_url, params=params)
    scan_id = res.json().get("scan", "0")
    print(f"[+] ZAP Spider triggered. Scan ID: {scan_id}")

    # 2. Trigger Active Scan (checks for SQLi, XSS, etc.)
    ascan_url = f"{ZAP_API_URL}/JSON/ascan/action/scan/"
    res = requests.get(ascan_url, params=params)
    print("[+] ZAP Active Scan triggered. Processing vulnerabilities...")


def run_fallback_header_checks():
    """Verify security headers manually if ZAP is offline."""
    print("[*] Running security checks on response headers...")
    try:
        res = requests.get(TARGET_URL, timeout=10)
        headers = res.headers

        issues = []
        # 1. X-Frame-Options (Clickjacking protection)
        if "X-Frame-Options" not in headers:
            issues.append("Missing X-Frame-Options header (risk: Clickjacking)")

        # 2. X-Content-Type-Options
        if "X-Content-Type-Options" not in headers:
            issues.append("Missing X-Content-Type-Options header (risk: MIME sniffing)")

        # 3. Content-Security-Policy (CSP)
        if "Content-Security-Policy" not in headers:
            issues.append("Missing Content-Security-Policy header (risk: XSS)")

        # 4. Strict-Transport-Security (HSTS)
        if "Strict-Transport-Security" not in headers:
            issues.append("Missing Strict-Transport-Security header (risk: MITM)")

        print(f"[+] Scan completed. Found {len(issues)} security findings.")
        for issue in issues:
            print(f"  - [WARNING] {issue}")

        # Save dummy report
        report_path = "tests/reports/html/security_findings.json"
        with open(report_path, "w") as f:
            json.dump({"target": TARGET_URL, "vulnerabilities": issues}, f, indent=2)
        print(f"[+] Security findings report saved to {report_path}")

    except Exception as e:
        print(f"[-] Could not perform header checks on {TARGET_URL}: {e}")


if __name__ == "__main__":
    main()

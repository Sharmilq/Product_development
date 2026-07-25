import os
import pandas as pd

def generate_security_excels():
    output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'Vulnerability Test Results')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # 1. Security Findings Data
    findings_data = [
        {
            'Finding ID': 'SEC-001',
            'Severity': 'High',
            'Vulnerability Type': 'Broken Access Control',
            'File Path': 'backend/server.js:49',
            'Endpoint': '/auth/*',
            'Description': 'Service Role Key bypasses Supabase Row Level Security (RLS)',
            'Impact': 'Privilege escalation and unauthorized data modification across accounts',
            'Recommended Fix': 'Scope database client permissions and use user-level RLS policies'
        },
        {
            'Finding ID': 'SEC-002',
            'Severity': 'High',
            'Vulnerability Type': 'Hardcoded Credentials',
            'File Path': 'app.py:39',
            'Endpoint': 'N/A',
            'Description': 'Hardcoded fallback Supabase Project URL in Python source code',
            'Impact': 'Project infrastructure URL exposed in repository',
            'Recommended Fix': 'Strictly enforce environment variables and crash on missing keys'
        },
        {
            'Finding ID': 'SEC-003',
            'Severity': 'Medium',
            'Vulnerability Type': 'Insecure Rate Limiter',
            'File Path': 'backend/server.js:102',
            'Endpoint': '/auth/request-password-otp',
            'Description': 'In-memory Map rate limiter resets on server restart/scaling',
            'Impact': 'Rate limit bypass leading to email/SMS quota exhaustion',
            'Recommended Fix': 'Use Redis-backed centralized rate limiting'
        },
        {
            'Finding ID': 'SEC-004',
            'Severity': 'Medium',
            'Vulnerability Type': 'Race Condition / File Overwrite',
            'File Path': 'app.py:185',
            'Endpoint': '/predict-tooth',
            'Description': 'Temporary tooth image saved to static temp_tooth.jpg path',
            'Impact': 'Concurrent user uploads overwrite each other causing cross-session data leakage',
            'Recommended Fix': 'Use UUID or tempfile.NamedTemporaryFile() for unique paths'
        },
        {
            'Finding ID': 'SEC-005',
            'Severity': 'Medium',
            'Vulnerability Type': 'Unrestricted File Upload',
            'File Path': 'app.py:184-188',
            'Endpoint': '/predict-tooth',
            'Description': 'Lack of file size limits and MIME type header verification',
            'Impact': 'Disk space exhaustion and potential execution of malicious files',
            'Recommended Fix': 'Configure MAX_CONTENT_LENGTH and validate image headers using PIL'
        },
        {
            'Finding ID': 'SEC-006',
            'Severity': 'Medium',
            'Vulnerability Type': 'Missing Security Headers',
            'File Path': 'backend/server.js:8',
            'Endpoint': 'All Endpoints',
            'Description': 'Missing Helmet middleware, CSP, and HSTS headers',
            'Impact': 'Increased susceptibility to XSS, clickjacking, and MIME-sniffing',
            'Recommended Fix': 'Install and configure Helmet.js in Express'
        },
        {
            'Finding ID': 'SEC-007',
            'Severity': 'Medium',
            'Vulnerability Type': 'Error Log Leakage',
            'File Path': 'backend/server.js:156',
            'Endpoint': '/auth/request-password-otp',
            'Description': 'Verbose API error messages printed to server logs',
            'Impact': 'Internal stack traces and API keys logged',
            'Recommended Fix': 'Sanitize error logging output in production'
        },
        {
            'Finding ID': 'SEC-008',
            'Severity': 'Low',
            'Vulnerability Type': 'Unsalted Hashing',
            'File Path': 'backend/server.js:117',
            'Endpoint': '/auth/request-password-otp',
            'Description': 'SHA-256 OTP hashing without dynamic salt',
            'Impact': 'Precomputed rainbow table attacks if OTP table leaked',
            'Recommended Fix': 'Add email or random salt to OTP hash algorithm'
        },
        {
            'Finding ID': 'SEC-009',
            'Severity': 'Low',
            'Vulnerability Type': 'Permissive CORS',
            'File Path': 'app.py:18',
            'Endpoint': 'All Endpoints',
            'Description': 'Flask app initializes CORS without domain restrictions',
            'Impact': 'Cross-origin requests permitted from any domain',
            'Recommended Fix': 'Restrict CORS origins to trusted frontend domains'
        },
        {
            'Finding ID': 'SEC-010',
            'Severity': 'Low',
            'Vulnerability Type': 'Unbounded Logging',
            'File Path': 'backend/server.js:25',
            'Endpoint': 'All Endpoints',
            'Description': 'Full HTTP response bodies logged to console',
            'Impact': 'Log storage exhaustion and verbose logging',
            'Recommended Fix': 'Truncate response logs to maximum 200 characters'
        }
    ]

    # 2. Endpoint Inventory Data
    endpoints_data = [
        {'Endpoint': '/', 'HTTP Method': 'GET, HEAD', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'app.py:24'},
        {'Endpoint': '/health', 'HTTP Method': 'GET', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'app.py:32'},
        {'Endpoint': '/predict', 'HTTP Method': 'POST', 'Authentication Required': 'No', 'Expected Roles': 'Public / User', 'Controller/File Path': 'app.py:151'},
        {'Endpoint': '/predict-tooth', 'HTTP Method': 'POST', 'Authentication Required': 'No', 'Expected Roles': 'Public / User', 'Controller/File Path': 'app.py:178'},
        {'Endpoint': '/auth/request-password-otp', 'HTTP Method': 'POST', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'backend/server.js:161'},
        {'Endpoint': '/auth/verify-password-otp', 'HTTP Method': 'POST', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'backend/server.js:239'},
        {'Endpoint': '/auth/reset-password-with-otp', 'HTTP Method': 'POST', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'backend/server.js:285'},
        {'Endpoint': '/', 'HTTP Method': 'GET', 'Authentication Required': 'No', 'Expected Roles': 'Public', 'Controller/File Path': 'backend/server.js:383'}
    ]

    # 3. Dependency Vulnerabilities Data
    deps_data = [
        {'Package': 'express', 'Type': 'npm', 'Installed Version': '4.18.2', 'Vulnerability': 'Outdated core framework', 'Severity': 'Low', 'Recommendation': 'Upgrade to express 4.21.2+'},
        {'Package': 'cors', 'Type': 'npm', 'Installed Version': '2.8.5', 'Vulnerability': 'Wildcard origin permitted', 'Severity': 'Medium', 'Recommendation': 'Restrict origin whitelist'},
        {'Package': '@supabase/supabase-js', 'Type': 'npm', 'Installed Version': '2.39.7', 'Vulnerability': 'Service Role Key overuse', 'Severity': 'High', 'Recommendation': 'Restrict scope to anon key'},
        {'Package': 'flask', 'Type': 'pip', 'Installed Version': '3.0.2', 'Vulnerability': 'Development server WSGI warning', 'Severity': 'Medium', 'Recommendation': 'Use Gunicorn / Waitress'},
        {'Package': 'tensorflow', 'Type': 'pip', 'Installed Version': '2.15.0', 'Vulnerability': 'Memory safety patch available', 'Severity': 'Low', 'Recommendation': 'Upgrade to 2.16+'}
    ]

    # 4. Risk Summary Data
    risk_summary_data = [
        {'Metric': 'Total Findings', 'Value': 10},
        {'Metric': 'Critical Severity Findings', 'Value': 0},
        {'Metric': 'High Severity Findings', 'Value': 2},
        {'Metric': 'Medium Severity Findings', 'Value': 5},
        {'Metric': 'Low Severity Findings', 'Value': 3},
        {'Metric': 'Overall Security Score', 'Value': '78 / 100'},
        {'Metric': 'Assessment Status', 'Value': 'COMPLETED'}
    ]

    df_findings = pd.DataFrame(findings_data)
    df_endpoints = pd.DataFrame(endpoints_data)
    df_deps = pd.DataFrame(deps_data)
    df_risk = pd.DataFrame(risk_summary_data)

    # Output findings.xlsx
    findings_excel = os.path.join(output_dir, 'findings.xlsx')
    with pd.ExcelWriter(findings_excel, engine='openpyxl') as writer:
        df_findings.to_excel(writer, sheet_name='Security Findings', index=False)
        df_endpoints.to_excel(writer, sheet_name='Endpoint Inventory', index=False)
        df_deps.to_excel(writer, sheet_name='Dependency Vulnerabilities', index=False)
        df_risk.to_excel(writer, sheet_name='Risk Summary', index=False)

    # Output endpoint-inventory.xlsx
    inventory_excel = os.path.join(output_dir, 'endpoint-inventory.xlsx')
    with pd.ExcelWriter(inventory_excel, engine='openpyxl') as writer:
        df_endpoints.to_excel(writer, sheet_name='Endpoint Inventory', index=False)

    print(f"[SUCCESS] Generated Security Excel Files in: {output_dir}")

if __name__ == '__main__':
    generate_security_excels()

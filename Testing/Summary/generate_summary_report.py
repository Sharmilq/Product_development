import json
import csv
import os
import datetime

def generate_reports():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    web_report_path = os.path.join(base_dir, 'Web_Selenium', 'reports', 'web_test_report.json')
    summary_dir = os.path.join(base_dir, 'Summary')
    
    if not os.path.exists(summary_dir):
        os.makedirs(summary_dir)

    all_tests = []
    
    # Parse Web Tests
    if os.path.exists(web_report_path):
        with open(web_report_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            for suite in data['results']:
                for nested_suite in suite['suites']:
                    suite_title = nested_suite['title']
                    for test in nested_suite['tests']:
                        status = 'PASS' if test['pass'] else 'FAIL' if test['fail'] else 'SKIPPED'
                        duration = test.get('duration', 0)
                        all_tests.append({
                            'Module': 'Web Selenium',
                            'Suite': suite_title,
                            'Test Case': test['title'],
                            'Status': status,
                            'Duration (ms)': duration
                        })

    # Output CSV
    csv_path = os.path.join(summary_dir, 'DentNova_Full_Test_Report.csv')
    with open(csv_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=['Module', 'Suite', 'Test Case', 'Status', 'Duration (ms)'])
        writer.writeheader()
        writer.writerows(all_tests)
        
    print(f"Generated CSV Report: {csv_path}")

    # Output HTML
    html_path = os.path.join(summary_dir, 'DentNova_Full_Test_Report.html')
    with open(html_path, 'w', encoding='utf-8') as f:
        f.write("<html><head><title>DentNova Enterprise Test Report</title>")
        f.write("<style>body{font-family: Arial, sans-serif;} table{border-collapse: collapse; width: 100%;} th, td{border: 1px solid #ddd; padding: 8px;} th{background-color: #f2f2f2;} .PASS{color: green;} .FAIL{color: red;}</style>")
        f.write("</head><body><h1>DentNova Complete QA Test Register</h1>")
        f.write(f"<p>Generated on: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>")
        f.write("<table><tr><th>Module</th><th>Suite</th><th>Test Case</th><th>Status</th><th>Duration (ms)</th></tr>")
        
        for t in all_tests:
            f.write(f"<tr><td>{t['Module']}</td><td>{t['Suite']}</td><td>{t['Test Case']}</td><td class='{t['Status']}'><b>{t['Status']}</b></td><td>{t['Duration (ms)']}</td></tr>")
        
        f.write("</table></body></html>")

    print(f"Generated HTML Report: {html_path}")

if __name__ == '__main__':
    generate_reports()

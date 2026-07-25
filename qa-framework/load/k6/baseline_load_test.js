import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 }, // Ramp-up to 100 VUs
    { duration: '40s', target: 100 }, // Stay at 100 VUs for 40s
    { duration: '10s', target: 0 },   // Ramp-down to 0 VUs
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate must be under 1%
  },
};

const BASE_URL = __ENV.TARGET_URL || 'https://dentnova-ml.onrender.com';

export default function () {
  // Scenario 1: Health check endpoint
  const resHealth = http.get(`${BASE_URL}/health`);
  check(resHealth, {
    'health status is 200': (r) => r.status === 200,
    'health body is healthy': (r) => r.json().status === 'healthy',
  });

  // Scenario 2: Assessment ML prediction endpoint
  const payload = JSON.stringify({
    age: 25,
    gender: 1,
    brush_frequency: 2,
    floss_frequency: 1,
    sugar_intake: 2,
    smoking: 0,
    alcohol: 0,
    bleeding_gums: 0,
    tooth_sensitivity: 0,
    last_dental_visit: 6
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const resPredict = http.post(`${BASE_URL}/predict`, payload, params);
  check(resPredict, {
    'predict status is 200': (r) => r.status === 200,
    'predict has score': (r) => r.json().score !== undefined,
  });

  sleep(0.5); // Pace requests realistically
}

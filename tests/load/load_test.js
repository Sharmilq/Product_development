import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp-up to 20 users
    { duration: '1m', target: 50 },   // Ramp-up to 50 users
    { duration: '1m', target: 100 },  // Ramp-up to 100 users
    { duration: '30s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'], // 95% of requests must complete under 1.5s
    http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
  },
};

const BASE_URL = __ENV.TEST_OTP_URL || 'https://dentnova-otp-backend.onrender.com';
const ML_URL = __ENV.TEST_ML_URL || 'https://dentnova-ml.onrender.com';

export default function () {
  // Scenario 1: Health check
  const resHealth = http.get(`${BASE_URL}/`);
  check(resHealth, {
    'health check status is 200': (r) => r.status === 200,
    'health check has success true': (r) => r.json('success') === true,
  });
  sleep(1);

  // Scenario 2: Request OTP
  const payloadOtp = JSON.stringify({ email: `load_test_${__VU}_${__ITER}@example.com` });
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  const resOtp = http.post(`${BASE_URL}/auth/request-password-otp`, payloadOtp, params);
  check(resOtp, {
    'OTP request status code is 200 or 404 or 429': (r) => [200, 404, 429].includes(r.status),
  });
  sleep(1);

  // Scenario 3: Assessment Risk Prediction
  const payloadRisk = JSON.stringify({
    answers: [0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0],
  });
  const resRisk = http.post(`${ML_URL}/predict-risk`, payloadRisk, params);
  check(resRisk, {
    'ML risk prediction status is 200 or 404': (r) => [200, 404].includes(r.status),
  });
  sleep(1);
}

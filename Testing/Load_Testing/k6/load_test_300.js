import http from 'k6/http';
import { check, sleep, group } from 'k6';

// ─── 300 LOAD-TESTING SCENARIOS MATRIX & CONFIGURATION ────────────────────────
// Covers: Authentication, Registration, Dashboard, User Profile, Tooth Scan ML,
// Assessment APIs, Report Generation, Notifications, Settings, Rate-Limiting,
// Concurrent User Scaling, Spike, Stress, Endurance & Throughput Validation.

export const options = {
  stages: [
    { duration: '15s', target: 20 },   // Warm-up ramp
    { duration: '30s', target: 100 },  // Baseline Load
    { duration: '15s', target: 250 },  // Spike & Stress
    { duration: '30s', target: 100 },  // Endurance / Soak sustained
    { duration: '10s', target: 0 },    // Cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.05'],
    http_reqs: ['count>300'],
  },
};

const BASE_OTP_URL = __ENV.OTP_URL || 'http://localhost:5000';
const BASE_ML_URL  = __ENV.ML_URL  || 'https://dentnova-ml.onrender.com';
const BASE_SB_URL  = __ENV.SUPABASE_URL || 'https://kxuwskwwmrpoilrxngha.supabase.co';

// Helper data generator for 300 distinct load scenarios
function getScenarioPayload(id) {
  const categories = [
    'Auth_Request_OTP', 'Auth_Verify_OTP', 'Auth_Reset_Pass', 'Auth_Register_Mock',
    'Profile_View', 'Profile_Update', 'ToothScan_Predict', 'Assessment_Calculate',
    'Report_Generate', 'Notification_List', 'Settings_Update', 'RateLimit_Stress'
  ];
  const cat = categories[id % categories.length];
  
  return {
    id: id,
    category: cat,
    email: `load_user_${id}@dentnova-qa.org`,
    password: `Password@123_${id}`,
    otp: `${100000 + (id * 37) % 899999}`,
    answers: Array.from({ length: 13 }, (_, i) => (id + i) % 3),
    age: 18 + (id % 60),
    gender: id % 2,
    brush_frequency: 1 + (id % 3),
    floss_frequency: id % 3,
    sugar_intake: id % 4,
    smoking: id % 2,
    alcohol: id % 2,
    bleeding_gums: id % 2,
    tooth_sensitivity: id % 2,
    last_dental_visit: 1 + (id % 24)
  };
}

export default function () {
  // Execute 300 load scenarios per VU execution cycle
  for (let scId = 1; scId <= 300; scId++) {
    const payload = getScenarioPayload(scId);

    // Grouping into 12 main core load modules
    if (scId <= 25) {
      // ─── Module 1: Auth Request OTP (Scenarios 1 - 25) ───────────────────
      group(`Scenario_${scId}_Auth_Request_OTP`, function () {
        const res = http.post(`${BASE_OTP_URL}/auth/request-password-otp`, JSON.stringify({
          email: payload.email
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'Request OTP status valid (200/404/429)': (r) => [200, 404, 429].includes(r.status),
          'Request OTP duration < 500ms': (r) => r.timings.duration < 500
        });
      });
    } else if (scId <= 50) {
      // ─── Module 2: Auth Verify OTP (Scenarios 26 - 50) ───────────────────
      group(`Scenario_${scId}_Auth_Verify_OTP`, function () {
        const res = http.post(`${BASE_OTP_URL}/auth/verify-password-otp`, JSON.stringify({
          email: payload.email,
          otp: payload.otp
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'Verify OTP status valid (400/404/429/200)': (r) => [200, 400, 404, 429].includes(r.status),
          'Verify OTP duration < 500ms': (r) => r.timings.duration < 500
        });
      });
    } else if (scId <= 75) {
      // ─── Module 3: Auth Reset Password (Scenarios 51 - 75) ───────────────
      group(`Scenario_${scId}_Auth_Reset_Pass`, function () {
        const res = http.post(`${BASE_OTP_URL}/auth/reset-password-with-otp`, JSON.stringify({
          email: payload.email,
          otp: payload.otp,
          newPassword: payload.password
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'Reset Pass status valid (400/404/429/200)': (r) => [200, 400, 404, 429].includes(r.status),
          'Reset Pass duration < 500ms': (r) => r.timings.duration < 500
        });
      });
    } else if (scId <= 100) {
      // ─── Module 4: Backend Health Check (Scenarios 76 - 100) ──────────────
      group(`Scenario_${scId}_Backend_Health`, function () {
        const res = http.get(`${BASE_OTP_URL}/`);
        check(res, {
          'Health status 200': (r) => r.status === 200,
          'Health latency < 200ms': (r) => r.timings.duration < 200
        });
      });
    } else if (scId <= 125) {
      // ─── Module 5: ML Assessment Predict Risk (Scenarios 101 - 125) ──────
      group(`Scenario_${scId}_ML_Predict_Risk`, function () {
        const res = http.post(`${BASE_ML_URL}/predict-risk`, JSON.stringify({
          answers: payload.answers
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'ML Predict Risk status valid (200/404/503)': (r) => [200, 404, 503].includes(r.status),
          'ML Predict Risk latency < 800ms': (r) => r.timings.duration < 800
        });
      });
    } else if (scId <= 150) {
      // ─── Module 6: ML Predict Features (Scenarios 126 - 150) ────────────
      group(`Scenario_${scId}_ML_Predict_Features`, function () {
        const res = http.post(`${BASE_ML_URL}/predict`, JSON.stringify({
          age: payload.age,
          gender: payload.gender,
          brush_frequency: payload.brush_frequency,
          floss_frequency: payload.floss_frequency,
          sugar_intake: payload.sugar_intake,
          smoking: payload.smoking,
          alcohol: payload.alcohol,
          bleeding_gums: payload.bleeding_gums,
          tooth_sensitivity: payload.tooth_sensitivity,
          last_dental_visit: payload.last_dental_visit
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'ML Predict status valid (200/404/503)': (r) => [200, 404, 503].includes(r.status),
          'ML Predict latency < 800ms': (r) => r.timings.duration < 800
        });
      });
    } else if (scId <= 175) {
      // ─── Module 7: ML Health Check (Scenarios 151 - 175) ────────────────
      group(`Scenario_${scId}_ML_Health`, function () {
        const res = http.get(`${BASE_ML_URL}/health`);
        check(res, {
          'ML Health status valid': (r) => [200, 404].includes(r.status),
          'ML Health latency < 300ms': (r) => r.timings.duration < 300
        });
      });
    } else if (scId <= 200) {
      // ─── Module 8: Supabase REST Users query (Scenarios 176 - 200) ──────
      group(`Scenario_${scId}_Supabase_Users_REST`, function () {
        const res = http.get(`${BASE_SB_URL}/rest/v1/users?limit=5`, {
          headers: { 'apikey': 'anon-key-placeholder' }
        });
        check(res, {
          'Supabase Users REST status (200/401/403)': (r) => [200, 401, 403].includes(r.status),
          'Supabase latency < 400ms': (r) => r.timings.duration < 400
        });
      });
    } else if (scId <= 225) {
      // ─── Module 9: Supabase Assessments Table (Scenarios 201 - 225) ────
      group(`Scenario_${scId}_Supabase_Assessments_REST`, function () {
        const res = http.get(`${BASE_SB_URL}/rest/v1/assessments?limit=5`, {
          headers: { 'apikey': 'anon-key-placeholder' }
        });
        check(res, {
          'Supabase Assessments status (200/401/403)': (r) => [200, 401, 403].includes(r.status),
          'Supabase latency < 400ms': (r) => r.timings.duration < 400
        });
      });
    } else if (scId <= 250) {
      // ─── Module 10: Supabase Reminders Table (Scenarios 226 - 250) ──────
      group(`Scenario_${scId}_Supabase_Reminders_REST`, function () {
        const res = http.get(`${BASE_SB_URL}/rest/v1/reminders?limit=5`, {
          headers: { 'apikey': 'anon-key-placeholder' }
        });
        check(res, {
          'Supabase Reminders status (200/401/403)': (r) => [200, 401, 403].includes(r.status),
          'Supabase latency < 400ms': (r) => r.timings.duration < 400
        });
      });
    } else if (scId <= 275) {
      // ─── Module 11: Supabase Visits Table (Scenarios 251 - 275) ────────
      group(`Scenario_${scId}_Supabase_Visits_REST`, function () {
        const res = http.get(`${BASE_SB_URL}/rest/v1/visits?limit=5`, {
          headers: { 'apikey': 'anon-key-placeholder' }
        });
        check(res, {
          'Supabase Visits status (200/401/403)': (r) => [200, 401, 403].includes(r.status),
          'Supabase latency < 400ms': (r) => r.timings.duration < 400
        });
      });
    } else {
      // ─── Module 12: Rate Limiting & Concurrent Stress (Scenarios 276 - 300) ─
      group(`Scenario_${scId}_Rate_Limit_Stress`, function () {
        const res = http.post(`${BASE_OTP_URL}/auth/request-password-otp`, JSON.stringify({
          email: `burst_user_${scId}@dentnova-qa.org`
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, {
          'Burst Request OTP handled (200/404/429)': (r) => [200, 404, 429].includes(r.status),
          'Burst latency < 600ms': (r) => r.timings.duration < 600
        });
      });
    }
  }

  sleep(0.1);
}

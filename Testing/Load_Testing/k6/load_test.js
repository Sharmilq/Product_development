import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 }, // simulate ramp-up of traffic from 1 to 20 users over 30s.
        { duration: '1m', target: 20 }, // stay at 20 users for 1 minute
        { duration: '30s', target: 0 }, // ramp-down to 0 users
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
        http_req_failed: ['rate<0.01'], // http errors should be less than 1%
    },
};

export default function () {
    const baseUrl = 'http://localhost:5000'; // Flask backend

    const res = http.get(`${baseUrl}/api/status`); // Adjust to a valid endpoint
    check(res, {
        'status is 200': (r) => r.status === 200,
        'transaction time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);
}

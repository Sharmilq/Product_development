import load300Options, { default as main300 } from './load_test_300.js';

export const options = {
  stages: [
    { duration: '5s', target: 10 },
    { duration: '15s', target: 50 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  main300();
}

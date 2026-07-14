/**
 * Backend OTP unit tests using Jest
 */
const crypto = require('crypto');

function sha256(text) {
  return crypto.createHash('sha256').update(text).digest('hex');
}

function isRateLimitedMock(email, rateLimitsMap) {
  const now = Date.now();
  const WINDOW_MS = 15 * 60 * 1000;
  const MAX_REQUESTS = 3;
  const entry = rateLimitsMap.get(email);
  if (!entry || now - entry.windowStart > WINDOW_MS) {
    rateLimitsMap.set(email, { count: 1, windowStart: now });
    return false;
  }
  if (entry.count >= MAX_REQUESTS) return true;
  entry.count++;
  return false;
}

describe('Backend Server Helper Tests', () => {
  let rateLimits;

  beforeEach(() => {
    rateLimits = new Map();
  });

  it('should generate valid SHA256 hashes', () => {
    const hash = sha256("123456");
    expect(hash).toBe("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
  });

  it('should rate limit emails requesting OTP too frequently', () => {
    const email = "user@test.com";

    // 1st request - allowed
    expect(isRateLimitedMock(email, rateLimits)).toBe(false);
    // 2nd request - allowed
    expect(isRateLimitedMock(email, rateLimits)).toBe(false);
    // 3rd request - allowed
    expect(isRateLimitedMock(email, rateLimits)).toBe(false);
    // 4th request - blocked
    expect(isRateLimitedMock(email, rateLimits)).toBe(true);
  });

  it('should reset rate limit window after expiry', () => {
    const email = "user@test.com";
    rateLimits.set(email, { count: 3, windowStart: Date.now() - 20 * 60 * 1000 }); // expired window

    // Next request should reset and be allowed
    expect(isRateLimitedMock(email, rateLimits)).toBe(false);
    expect(rateLimits.get(email).count).toBe(1);
  });
});

/**
 * Web Frontend unit tests for utility functions
 */
import { describe, it, expect } from 'vitest';
import { getJavaHashCode } from '../../../dentnova-web/src/lib/utils';

// Helper to parse dates in web format
function parseVisitDateTime(dateStr, timeStr) {
  try {
    const MONTHS = { Jan:0, Feb:1, Mar:2, Apr:3, May:4, Jun:5,
                     Jul:6, Aug:7, Sep:8, Oct:9, Nov:10, Dec:11 }
    const [dd, mon, yyyy] = (dateStr || '').trim().split(' ')
    const timeTrimmed = (timeStr || '12:00 AM').trim()
    const spaceIdx = timeTrimmed.lastIndexOf(' ')
    const timePart = spaceIdx >= 0 ? timeTrimmed.slice(0, spaceIdx) : timeTrimmed
    const meridiem = spaceIdx >= 0 ? timeTrimmed.slice(spaceIdx + 1).toUpperCase() : ''
    const [hhStr, mmStr] = timePart.split(':')
    let hour = parseInt(hhStr, 10)
    const minute = parseInt(mmStr, 10)
    if (isNaN(hour) || isNaN(minute)) return new Date(NaN)
    if (meridiem === 'PM' && hour !== 12) hour += 12
    if (meridiem === 'AM' && hour === 12) hour = 0
    const monthIndex = MONTHS[mon]
    if (monthIndex === undefined) return new Date(NaN)
    return new Date(parseInt(yyyy, 10), monthIndex, parseInt(dd, 10), hour, minute, 0, 0)
  } catch {
    return new Date(NaN)
  }
}

describe('Web Utility Function Tests', () => {
  it('should compute exact Java hashCode for email strings', () => {
    // Tests that getJavaHashCode generates identical integer values as Java String.hashCode()
    const code1 = getJavaHashCode("test@dentnova.com");
    expect(code1).toBe(467884179); // Pre-computed Java hash for this email

    const code2 = getJavaHashCode("another.email+alias@gmail.com");
    expect(typeof code2).toBe('number');
    expect(Number.isInteger(code2)).toBe(true);
  });

  it('should correctly parse Android localized visit date and time', () => {
    const parsed = parseVisitDateTime("16 Jun 2026", "02:48 PM");
    expect(parsed.getFullYear()).toBe(2026);
    expect(parsed.getMonth()).toBe(5); // June is 5 in JS Date
    expect(parsed.getDate()).toBe(16);
    expect(parsed.getHours()).toBe(14); // 02 PM -> 14
    expect(parsed.getMinutes()).toBe(48);
  });

  it('should return invalid Date on bad input strings', () => {
    const invalid = parseVisitDateTime("bad date", "bad time");
    expect(isNaN(invalid.getTime())).toBe(true);
  });
});

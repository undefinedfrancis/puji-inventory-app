import assert from 'node:assert/strict';
import test from 'node:test';
import {
  customDateRange,
  monthRange,
  parseLocalDate,
  quarterRange,
  yearRange
} from '../entry/src/main/ets/domain/Periods.ets';

test('builds a local January range', () => {
  const range = monthRange(new Date(2026, 0, 18, 12).getTime());
  assert.deepEqual(range, {
    startInclusive: new Date(2026, 0, 1).getTime(),
    endExclusive: new Date(2026, 1, 1).getTime()
  });
});

test('builds the second-quarter range', () => {
  const range = quarterRange(new Date(2026, 4, 8, 12).getTime());
  assert.deepEqual(range, {
    startInclusive: new Date(2026, 3, 1).getTime(),
    endExclusive: new Date(2026, 6, 1).getTime()
  });
});

test('includes leap day in February', () => {
  const range = monthRange(new Date(2028, 1, 29, 12).getTime());
  assert.equal(range.endExclusive, new Date(2028, 2, 1).getTime());
});

test('builds a full year across the year boundary', () => {
  const range = yearRange(new Date(2026, 11, 31, 23, 59).getTime());
  assert.deepEqual(range, {
    startInclusive: new Date(2026, 0, 1).getTime(),
    endExclusive: new Date(2027, 0, 1).getTime()
  });
});

test('normalizes a custom range to inclusive local calendar days', () => {
  const range = customDateRange(
    new Date(2026, 8, 2, 15).getTime(),
    new Date(2026, 8, 5, 2).getTime()
  );
  assert.deepEqual(range, {
    startInclusive: new Date(2026, 8, 2).getTime(),
    endExclusive: new Date(2026, 8, 6).getTime()
  });
});

test('rejects a reversed custom range', () => {
  assert.equal(customDateRange(
    new Date(2026, 8, 5).getTime(),
    new Date(2026, 8, 2).getTime()
  ), null);
});

test('parses a strict local calendar date', () => {
  assert.equal(parseLocalDate('2026-09-08'), new Date(2026, 8, 8).getTime());
});

test('rejects impossible or loosely formatted dates', () => {
  assert.equal(parseLocalDate('2026-02-30'), null);
  assert.equal(parseLocalDate('2026-9-8'), null);
  assert.equal(parseLocalDate('not-a-date'), null);
});

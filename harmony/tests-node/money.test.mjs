import assert from 'node:assert/strict';
import test from 'node:test';
import {
  formatMoney,
  lineTotal,
  parseMoneyToCents
} from '../entry/src/main/ets/domain/Money.ets';

test('formats integer cents', () => {
  assert.equal(formatMoney(12345), '¥123.45');
  assert.equal(formatMoney(-30), '-¥0.30');
});

test('parses valid money text to integer cents', () => {
  assert.equal(parseMoneyToCents('12.30'), 1230);
  assert.equal(parseMoneyToCents(' 8 '), 800);
});

test('rejects invalid and negative money text', () => {
  assert.equal(parseMoneyToCents('12.345'), null);
  assert.equal(parseMoneyToCents('-1'), null);
  assert.equal(parseMoneyToCents(''), null);
});

test('calculates line totals with integer cents', () => {
  assert.equal(lineTotal(399, 5), 1995);
});

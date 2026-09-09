import assert from 'node:assert/strict';
import test from 'node:test';
import { HoldToConfirm } from '../entry/src/main/ets/domain/HoldToConfirm.ets';

test('does not confirm before two seconds', () => {
  const hold = new HoldToConfirm(2000);
  hold.press(1000);
  assert.equal(hold.tick(2999), false);
});

test('confirms once after two seconds', () => {
  const hold = new HoldToConfirm(2000);
  hold.press(1000);
  assert.equal(hold.tick(3000), true);
  assert.equal(hold.tick(4000), false);
});

test('release cancels the pending confirmation', () => {
  const hold = new HoldToConfirm(2000);
  hold.press(1000);
  hold.release();
  assert.equal(hold.tick(5000), false);
  assert.equal(hold.progress(5000), 0);
});

test('reports clamped progress from zero to one', () => {
  const hold = new HoldToConfirm(2000);
  assert.equal(hold.progress(500), 0);
  hold.press(1000);
  assert.equal(hold.progress(1500), 0.25);
  assert.equal(hold.progress(4000), 1);
});

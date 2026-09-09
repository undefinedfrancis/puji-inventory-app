import assert from 'node:assert/strict';
import test from 'node:test';
import { LatestRequestGate } from '../entry/src/main/ets/domain/LatestRequestGate.ets';

test('accepts only the newest asynchronous request result', () => {
  const gate = new LatestRequestGate();
  const first = gate.begin();
  const second = gate.begin();

  assert.equal(gate.isLatest(first), false);
  assert.equal(gate.isLatest(second), true);
});

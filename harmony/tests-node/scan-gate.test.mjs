import assert from 'node:assert/strict';
import test from 'node:test';
import { ScanGate } from '../entry/src/main/ets/domain/ScanGate.ets';

test('accepts one normalized barcode and locks immediately', () => {
  const gate = new ScanGate();
  assert.equal(gate.accept(' 6901 '), '6901');
  assert.equal(gate.isLocked(), true);
});

test('rejects duplicate callbacks while locked', () => {
  const gate = new ScanGate();
  assert.equal(gate.accept('6901'), '6901');
  assert.equal(gate.accept('6901'), null);
  assert.equal(gate.accept('6902'), null);
});

test('complete unlocks the next scan', () => {
  const gate = new ScanGate();
  gate.accept('6901');
  gate.complete();
  assert.equal(gate.accept('6902'), '6902');
});

test('cancel unlocks and empty values never lock', () => {
  const gate = new ScanGate();
  assert.equal(gate.accept('   '), null);
  assert.equal(gate.isLocked(), false);
  gate.accept('6901');
  gate.cancel();
  assert.equal(gate.isLocked(), false);
});

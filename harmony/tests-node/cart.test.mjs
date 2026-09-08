import assert from 'node:assert/strict';
import test from 'node:test';
import { Cart } from '../entry/src/main/ets/domain/Cart.ets';
import { DocumentType } from '../entry/src/main/ets/model/Models.ets';

const cola = {
  id: 1,
  name: '可口可乐 500ml',
  barcode: '6901234567890',
  category: '饮料',
  costPriceCents: 220,
  salePriceCents: 350,
  stockQuantity: 12,
  lowStockThreshold: 5,
  note: '',
  active: true,
  createdAt: 0,
  updatedAt: 0
};

test('adds exact scanned inbound quantity without an implicit extra item', () => {
  const cart = Cart.empty(DocumentType.INBOUND).add(cola, 5);
  assert.equal(cart.lines[0].quantity, 5);
  assert.equal(cart.totalCents(), 1100);
});

test('merges repeated products and uses the outbound sale price', () => {
  const cart = Cart.empty(DocumentType.OUTBOUND).add(cola).add(cola);
  assert.equal(cart.lines.length, 1);
  assert.equal(cart.lines[0].quantity, 2);
  assert.equal(cart.totalCents(), 700);
});

test('uses cost price for inbound and supports a price override', () => {
  const cart = Cart.empty(DocumentType.INBOUND).add(cola).updateUnitPrice(cola.id, 200);
  assert.equal(cart.totalCents(), 200);
});

test('decrementing the last unit removes the line', () => {
  const cart = Cart.empty(DocumentType.OUTBOUND).add(cola).decrement(cola.id);
  assert.equal(cart.lines.length, 0);
});

test('cart operations return new instances', () => {
  const empty = Cart.empty(DocumentType.OUTBOUND);
  const added = empty.add(cola);
  assert.equal(empty.lines.length, 0);
  assert.equal(added.lines.length, 1);
});

test('rejects non-positive quantities and prices', () => {
  assert.throws(() => Cart.empty(DocumentType.INBOUND).add(cola, 0));
  assert.throws(() => Cart.empty(DocumentType.INBOUND).add(cola).updateUnitPrice(cola.id, -1));
});

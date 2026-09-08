import assert from 'node:assert/strict';
import test from 'node:test';
import { Cart } from '../entry/src/main/ets/domain/Cart.ets';
import { nextStock, validateCart } from '../entry/src/main/ets/domain/InventoryRules.ets';
import { DocumentType } from '../entry/src/main/ets/model/Models.ets';

const product = {
  id: 1,
  name: '矿泉水',
  barcode: '6902',
  category: '饮料',
  costPriceCents: 100,
  salePriceCents: 200,
  stockQuantity: 10,
  lowStockThreshold: 2,
  note: '',
  active: true,
  createdAt: 0,
  updatedAt: 0
};

test('calculates exact inbound and outbound stock', () => {
  assert.equal(nextStock(DocumentType.INBOUND, 10, 5), 15);
  assert.equal(nextStock(DocumentType.OUTBOUND, 10, 3), 7);
  assert.equal(nextStock(DocumentType.INBOUND, 0, 5), 5);
});

test('rejects insufficient outbound stock', () => {
  assert.equal(nextStock(DocumentType.OUTBOUND, 2, 3), null);
});

test('rejects empty carts and inactive products', () => {
  assert.equal(validateCart(Cart.empty(DocumentType.INBOUND)), '请先添加商品');
  const inactive = { ...product, active: false };
  assert.equal(validateCart(Cart.empty(DocumentType.INBOUND).add(inactive)), '商品“矿泉水”已停用');
});

test('rejects outbound carts that exceed the product stock snapshot', () => {
  const cart = Cart.empty(DocumentType.OUTBOUND).add(product, 11);
  assert.equal(validateCart(cart), '商品“矿泉水”库存不足');
});

test('accepts a valid exact-quantity inbound cart', () => {
  const fresh = { ...product, stockQuantity: 0 };
  assert.equal(validateCart(Cart.empty(DocumentType.INBOUND).add(fresh, 5)), null);
});

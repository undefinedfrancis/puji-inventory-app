import assert from 'node:assert/strict';
import test from 'node:test';
import { filterProducts } from '../entry/src/main/ets/domain/ProductFilter.ets';

const PRODUCTS = [
  { id: 1, name: '冰可乐', barcode: '6901', category: '饮料' },
  { id: 2, name: '矿泉水', barcode: '6902', category: '饮料' },
  { id: 3, name: '纸巾', barcode: '8801', category: '日用' }
];

test('filters the inventory view without changing the complete product collection', () => {
  const completeProducts = [...PRODUCTS];

  const visibleProducts = filterProducts(completeProducts, '690', '饮料');

  assert.deepEqual(visibleProducts.map((product) => product.id), [1, 2]);
  assert.deepEqual(completeProducts.map((product) => product.id), [1, 2, 3]);
});

test('matches product names and treats 全部 as no category filter', () => {
  assert.deepEqual(
    filterProducts(PRODUCTS, '纸', '全部').map((product) => product.id),
    [3]
  );
});

test('falls back to all products when a previously selected category no longer exists', () => {
  assert.deepEqual(
    filterProducts(PRODUCTS, '', '已清除的旧类别').map((product) => product.id),
    [1, 2, 3]
  );
});

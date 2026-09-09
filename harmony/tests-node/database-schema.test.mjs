import assert from 'node:assert/strict';
import test from 'node:test';
import {
  CREATE_SCHEMA_SQL,
  SCHEMA_VERSION,
  SEED_PRODUCTS,
  shouldSeedProducts
} from '../entry/src/main/ets/data/DatabaseSchema.ets';

test('creates all four business tables', () => {
  const sql = CREATE_SCHEMA_SQL.join('\n');
  assert.match(sql, /CREATE TABLE IF NOT EXISTS products/);
  assert.match(sql, /CREATE TABLE IF NOT EXISTS documents/);
  assert.match(sql, /CREATE TABLE IF NOT EXISTS document_lines/);
  assert.match(sql, /CREATE TABLE IF NOT EXISTS stock_movements/);
});

test('uses a partial unique index for non-empty barcodes', () => {
  const sql = CREATE_SCHEMA_SQL.join('\n');
  assert.match(sql, /UNIQUE INDEX IF NOT EXISTS idx_products_barcode/);
  assert.match(sql, /barcode IS NOT NULL AND barcode <> ''/);
});

test('keeps schema version one and the three approved sample products', () => {
  assert.equal(SCHEMA_VERSION, 1);
  assert.deepEqual(SEED_PRODUCTS.map((product) => product.name), [
    '可口可乐 500ml',
    '农夫山泉 550ml',
    '康师傅红烧牛肉面'
  ]);
});

test('seeds samples only for a brand-new database', () => {
  assert.equal(shouldSeedProducts(0), true);
  assert.equal(shouldSeedProducts(1), false);
});

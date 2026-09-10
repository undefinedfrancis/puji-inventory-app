import assert from 'node:assert/strict';
import test from 'node:test';
import {
  escapeCsv,
  exportDocuments,
  exportProducts
} from '../entry/src/main/ets/export/CsvExporter.ets';
import { DocumentType } from '../entry/src/main/ets/model/Models.ets';

test('escapes commas, quotes and line breaks', () => {
  assert.equal(escapeCsv('饮料,冷藏'), '"饮料,冷藏"');
  assert.equal(escapeCsv('大瓶"装'), '"大瓶""装"');
  assert.equal(escapeCsv('第一行\n第二行'), '"第一行\n第二行"');
});

test('neutralizes spreadsheet formulas in user-entered cells', () => {
  assert.equal(escapeCsv('=1+1'), "'=1+1");
  assert.equal(escapeCsv('+SUM(A1:A2)'), "'+SUM(A1:A2)");
  assert.equal(escapeCsv('-2+3'), "'-2+3");
  assert.equal(escapeCsv('@cmd'), "'@cmd");
  assert.equal(escapeCsv('\t=1+1'), "'\t=1+1");
  assert.equal(escapeCsv('  =1+1'), "'  =1+1");
});

test('exports products with UTF-8 BOM and stock columns', () => {
  const csv = exportProducts([{
    id: 1,
    name: '可乐,大瓶',
    barcode: '6901',
    category: '饮料',
    costPriceCents: 220,
    salePriceCents: 350,
    stockQuantity: 12,
    lowStockThreshold: 5,
    note: '冷藏',
    active: true,
    createdAt: 0,
    updatedAt: 0
  }]);
  assert.equal(csv.startsWith('\uFEFF商品名称,条码,类别,进货价,销售价,库存,低库存阈值,备注'), true);
  assert.equal(csv.includes('"可乐,大瓶",6901,饮料,2.20,3.50,12,5,冷藏'), true);
});

test('exports document cost and leaves inbound gross profit blank', () => {
  const inbound = {
    id: 8,
    type: DocumentType.INBOUND,
    totalCents: 500,
    note: '首批',
    createdAt: new Date(2026, 8, 2, 10).getTime(),
    lines: [{
      id: 1,
      documentId: 8,
      productId: 1,
      productName: '矿泉水',
      barcode: '6902',
      unitPriceCents: 250,
      costPriceSnapshotCents: 250,
      quantity: 2,
      lineTotalCents: 500
    }]
  };
  const csv = exportDocuments([inbound]);
  const row = csv.trim().split('\n')[1];
  assert.equal(csv.startsWith('\uFEFF单据号,类型,时间,商品名称,条码,数量,单价,金额,成本金额,毛利润,备注'), true);
  assert.equal(row.includes(',5.00,,'), true);
});

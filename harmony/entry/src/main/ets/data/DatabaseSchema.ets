export interface SeedProduct {
  name: string;
  barcode: string;
  category: string;
  costPriceCents: number;
  salePriceCents: number;
  stockQuantity: number;
  lowStockThreshold: number;
  note: string;
}

export const SCHEMA_VERSION: number = 1;

export function shouldSeedProducts(existingSchemaVersion: number): boolean {
  return existingSchemaVersion === 0;
}

export const CREATE_SCHEMA_SQL: string[] = [
  `CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    barcode TEXT,
    category TEXT NOT NULL DEFAULT '未分类',
    cost_price_cents INTEGER NOT NULL DEFAULT 0,
    sale_price_cents INTEGER NOT NULL DEFAULT 0,
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK(low_stock_threshold >= 0),
    note TEXT NOT NULL DEFAULT '',
    active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
  )`,
  `CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode
    ON products(barcode) WHERE barcode IS NOT NULL AND barcode <> ''`,
  'CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)',
  `CREATE TABLE IF NOT EXISTS documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL CHECK(type IN ('INBOUND','OUTBOUND')),
    total_cents INTEGER NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS document_lines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL REFERENCES documents(id),
    product_id INTEGER NOT NULL REFERENCES products(id),
    product_name_snapshot TEXT NOT NULL,
    barcode_snapshot TEXT NOT NULL DEFAULT '',
    unit_price_cents INTEGER NOT NULL,
    cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL CHECK(quantity > 0),
    line_total_cents INTEGER NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS stock_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL REFERENCES products(id),
    document_id INTEGER NOT NULL REFERENCES documents(id),
    type TEXT NOT NULL CHECK(type IN ('INBOUND','OUTBOUND')),
    quantity_delta INTEGER NOT NULL,
    stock_after INTEGER NOT NULL,
    created_at INTEGER NOT NULL
  )`,
  'CREATE INDEX IF NOT EXISTS idx_documents_created ON documents(created_at DESC)',
  'CREATE INDEX IF NOT EXISTS idx_documents_type_created ON documents(type, created_at DESC)',
  'CREATE INDEX IF NOT EXISTS idx_lines_document ON document_lines(document_id)',
  'CREATE INDEX IF NOT EXISTS idx_movements_product ON stock_movements(product_id, created_at DESC)'
];

export const SEED_PRODUCTS: SeedProduct[] = [{
  name: '可口可乐 500ml',
  barcode: '6901234567890',
  category: '饮料',
  costPriceCents: 220,
  salePriceCents: 350,
  stockQuantity: 18,
  lowStockThreshold: 5,
  note: '示例商品，可编辑'
}, {
  name: '农夫山泉 550ml',
  barcode: '6901234567891',
  category: '饮料',
  costPriceCents: 100,
  salePriceCents: 200,
  stockQuantity: 4,
  lowStockThreshold: 6,
  note: '低库存示例'
}, {
  name: '康师傅红烧牛肉面',
  barcode: '6901234567892',
  category: '食品',
  costPriceCents: 320,
  salePriceCents: 450,
  stockQuantity: 24,
  lowStockThreshold: 8,
  note: ''
}];

import { common } from '@kit.AbilityKit';
import { relationalStore } from '@kit.ArkData';
import {
  CREATE_SCHEMA_SQL,
  SCHEMA_VERSION,
  SEED_PRODUCTS,
  SeedProduct,
  shouldSeedProducts
} from './DatabaseSchema';

const STORE_CONFIG: relationalStore.StoreConfig = {
  name: 'puji.db',
  securityLevel: relationalStore.SecurityLevel.S1
};

export class PujiDatabase {
  private readonly context: common.Context;
  private store: relationalStore.RdbStore | null = null;

  constructor(context: common.Context) {
    this.context = context;
  }

  async getStore(): Promise<relationalStore.RdbStore> {
    if (this.store !== null) {
      return this.store;
    }
    const store = await relationalStore.getRdbStore(this.context, STORE_CONFIG);
    await this.initialize(store);
    this.store = store;
    return store;
  }

  private async initialize(store: relationalStore.RdbStore): Promise<void> {
    const versionResult = await store.querySql('PRAGMA user_version');
    let existingSchemaVersion = 0;
    if (versionResult.goToFirstRow()) {
      existingSchemaVersion = versionResult.getLong(0);
    }
    versionResult.close();

    await store.executeSql('PRAGMA foreign_keys = ON');
    for (let index = 0; index < CREATE_SCHEMA_SQL.length; index += 1) {
      await store.executeSql(CREATE_SCHEMA_SQL[index]);
    }

    if (shouldSeedProducts(existingSchemaVersion)) {
      const resultSet = await store.querySql('SELECT COUNT(*) AS product_count FROM products');
      let productCount = 0;
      if (resultSet.goToFirstRow()) {
        productCount = resultSet.getLong(resultSet.getColumnIndex('product_count'));
      }
      resultSet.close();
      if (productCount === 0) {
        await this.seed(store);
      }
    }
    await store.executeSql(`PRAGMA user_version = ${SCHEMA_VERSION}`);
  }

  private async seed(store: relationalStore.RdbStore): Promise<void> {
    const now = Date.now();
    for (let index = 0; index < SEED_PRODUCTS.length; index += 1) {
      const product: SeedProduct = SEED_PRODUCTS[index];
      const values: relationalStore.ValuesBucket = {
        'name': product.name,
        'barcode': product.barcode,
        'category': product.category,
        'cost_price_cents': product.costPriceCents,
        'sale_price_cents': product.salePriceCents,
        'stock_quantity': product.stockQuantity,
        'low_stock_threshold': product.lowStockThreshold,
        'note': product.note,
        'active': 1,
        'created_at': now,
        'updated_at': now
      };
      await store.insert('products', values);
    }
  }
}

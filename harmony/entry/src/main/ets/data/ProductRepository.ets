import { relationalStore } from '@kit.ArkData';
import type { CategoryCount, Product } from '../model/Models';
import { PujiDatabase } from './PujiDatabase';

export class ProductRepository {
  private readonly database: PujiDatabase;

  constructor(database: PujiDatabase) {
    this.database = database;
  }

  async list(
    query: string = '',
    category: string = '',
    includeInactive: boolean = false
  ): Promise<Product[]> {
    const clauses: string[] = [];
    const args: relationalStore.ValueType[] = [];
    if (!includeInactive) {
      clauses.push('active = 1');
    }
    const normalizedQuery = query.trim();
    if (normalizedQuery.length > 0) {
      clauses.push('(name LIKE ? OR barcode LIKE ?)');
      args.push(`%${normalizedQuery}%`);
      args.push(`%${normalizedQuery}%`);
    }
    const normalizedCategory = category.trim();
    if (normalizedCategory.length > 0 && normalizedCategory !== '全部') {
      clauses.push('category = ?');
      args.push(normalizedCategory);
    }
    const where = clauses.length === 0 ? '' : ` WHERE ${clauses.join(' AND ')}`;
    return this.queryProducts(`SELECT * FROM products${where} ORDER BY category, name`, args);
  }

  async findByBarcode(barcode: string): Promise<Product | null> {
    const normalized = barcode.trim();
    if (normalized.length === 0) {
      return null;
    }
    const products = await this.queryProducts(
      'SELECT * FROM products WHERE barcode = ? AND active = 1 LIMIT 1',
      [normalized]
    );
    return products.length === 0 ? null : products[0];
  }

  async findById(id: number): Promise<Product | null> {
    const products = await this.queryProducts(
      'SELECT * FROM products WHERE id = ? LIMIT 1',
      [id]
    );
    return products.length === 0 ? null : products[0];
  }

  async save(product: Product): Promise<Product> {
    const name = product.name.trim();
    if (name.length === 0) {
      throw new Error('商品名称不能为空');
    }
    if (product.costPriceCents < 0 || product.salePriceCents < 0) {
      throw new Error('价格不能小于 0');
    }
    if (product.stockQuantity < 0 || product.lowStockThreshold < 0) {
      throw new Error('库存和预警数量不能小于 0');
    }

    const store = await this.database.getStore();
    const now = Date.now();
    const createdAt = product.createdAt > 0 ? product.createdAt : now;
    const values: relationalStore.ValuesBucket = {
      'name': name,
      'barcode': product.barcode.trim(),
      'category': product.category.trim().length === 0 ? '未分类' : product.category.trim(),
      'cost_price_cents': Math.trunc(product.costPriceCents),
      'sale_price_cents': Math.trunc(product.salePriceCents),
      'stock_quantity': Math.trunc(product.stockQuantity),
      'low_stock_threshold': Math.trunc(product.lowStockThreshold),
      'note': product.note.trim(),
      'active': product.active ? 1 : 0,
      'created_at': createdAt,
      'updated_at': now
    };

    let id = product.id;
    if (id <= 0) {
      id = await store.insert('products', values);
    } else {
      const predicates = new relationalStore.RdbPredicates('products');
      predicates.equalTo('id', id);
      const changed = await store.update(values, predicates);
      if (changed !== 1) {
        throw new Error('商品不存在或已被删除');
      }
    }

    const saved = await this.findById(id);
    if (saved === null) {
      throw new Error('商品保存后读取失败');
    }
    return saved;
  }

  async categoryCounts(): Promise<CategoryCount[]> {
    const store = await this.database.getStore();
    const resultSet = await store.querySql(
      `SELECT category, COUNT(*) AS product_count
       FROM products WHERE active = 1 GROUP BY category ORDER BY category`
    );
    const counts: CategoryCount[] = [];
    try {
      while (resultSet.goToNextRow()) {
        counts.push({
          category: resultSet.getString(resultSet.getColumnIndex('category')),
          count: resultSet.getLong(resultSet.getColumnIndex('product_count'))
        });
      }
    } finally {
      resultSet.close();
    }
    return counts;
  }

  private async queryProducts(
    sql: string,
    args: relationalStore.ValueType[]
  ): Promise<Product[]> {
    const store = await this.database.getStore();
    const resultSet = await store.querySql(sql, args);
    const products: Product[] = [];
    try {
      while (resultSet.goToNextRow()) {
        products.push({
          id: resultSet.getLong(resultSet.getColumnIndex('id')),
          name: resultSet.getString(resultSet.getColumnIndex('name')),
          barcode: resultSet.getString(resultSet.getColumnIndex('barcode')),
          category: resultSet.getString(resultSet.getColumnIndex('category')),
          costPriceCents: resultSet.getLong(resultSet.getColumnIndex('cost_price_cents')),
          salePriceCents: resultSet.getLong(resultSet.getColumnIndex('sale_price_cents')),
          stockQuantity: resultSet.getLong(resultSet.getColumnIndex('stock_quantity')),
          lowStockThreshold: resultSet.getLong(resultSet.getColumnIndex('low_stock_threshold')),
          note: resultSet.getString(resultSet.getColumnIndex('note')),
          active: resultSet.getLong(resultSet.getColumnIndex('active')) === 1,
          createdAt: resultSet.getLong(resultSet.getColumnIndex('created_at')),
          updatedAt: resultSet.getLong(resultSet.getColumnIndex('updated_at'))
        });
      }
    } finally {
      resultSet.close();
    }
    return products;
  }
}

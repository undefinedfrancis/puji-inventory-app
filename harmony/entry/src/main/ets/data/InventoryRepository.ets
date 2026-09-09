import { relationalStore } from '@kit.ArkData';
import { Cart, CartLine } from '../domain/Cart';
import { nextStock, validateCart } from '../domain/InventoryRules';
import { DocumentType } from '../model/Models';
import type { DateRange, DocumentLine, Product, StockDocument } from '../model/Models';
import { PujiDatabase } from './PujiDatabase';
import { ProductRepository } from './ProductRepository';

export class InventoryRepository {
  private readonly database: PujiDatabase;
  private readonly productRepository: ProductRepository;

  constructor(database: PujiDatabase) {
    this.database = database;
    this.productRepository = new ProductRepository(database);
  }

  async commit(cart: Cart, note: string = ''): Promise<StockDocument> {
    const validation = validateCart(cart);
    if (validation !== null) {
      throw new Error(validation);
    }

    const store = await this.database.getStore();
    store.beginTransaction();
    try {
      const document = await this.persistDocument(store, cart, note);
      store.commit();
      return document;
    } catch (error) {
      store.rollBack();
      throw new Error(`${error}`);
    }
  }

  async createProductAndCommitInbound(product: Product, quantity: number): Promise<StockDocument> {
    const store = await this.database.getStore();
    store.beginTransaction();
    try {
      const saved = await this.productRepository.save({
        id: product.id,
        name: product.name,
        barcode: product.barcode,
        category: product.category,
        costPriceCents: product.costPriceCents,
        salePriceCents: product.salePriceCents,
        stockQuantity: 0,
        lowStockThreshold: product.lowStockThreshold,
        note: product.note,
        active: product.active,
        createdAt: product.createdAt,
        updatedAt: product.updatedAt
      });
      const cart = Cart.empty(DocumentType.INBOUND).add(saved, quantity);
      const validation = validateCart(cart);
      if (validation !== null) {
        throw new Error(validation);
      }
      const document = await this.persistDocument(store, cart, '');
      store.commit();
      return document;
    } catch (error) {
      store.rollBack();
      throw new Error(`${error}`);
    }
  }

  private async persistDocument(
    store: relationalStore.RdbStore,
    cart: Cart,
    note: string
  ): Promise<StockDocument> {
    const now = Date.now();
    const documentValues: relationalStore.ValuesBucket = {
        'type': cart.type,
        'total_cents': cart.totalCents(),
        'note': note.trim(),
        'created_at': now
      };
      const documentId = await store.insert('documents', documentValues);
      const savedLines: DocumentLine[] = [];

      for (let index = 0; index < cart.lines.length; index += 1) {
        const line: CartLine = cart.lines[index];
        const currentStock = await this.currentStock(store, line.product.id);
        const updatedStock = nextStock(cart.type, currentStock, line.quantity);
        if (updatedStock === null) {
          throw new Error(`商品“${line.product.name}”库存不足`);
        }

        const predicates = new relationalStore.RdbPredicates('products');
        predicates.equalTo('id', line.product.id);
        predicates.and();
        predicates.equalTo('stock_quantity', currentStock);
        const changed = await store.update({
          'stock_quantity': updatedStock,
          'updated_at': now
        }, predicates);
        if (changed !== 1) {
          throw new Error('库存已变化，请重试');
        }

        const costSnapshot = cart.type === DocumentType.INBOUND
          ? line.unitPriceCents
          : line.product.costPriceCents;
        const lineTotal = line.unitPriceCents * line.quantity;
        const lineId = await store.insert('document_lines', {
          'document_id': documentId,
          'product_id': line.product.id,
          'product_name_snapshot': line.product.name,
          'barcode_snapshot': line.product.barcode,
          'unit_price_cents': line.unitPriceCents,
          'cost_price_snapshot_cents': costSnapshot,
          'quantity': line.quantity,
          'line_total_cents': lineTotal
        });
        await store.insert('stock_movements', {
          'product_id': line.product.id,
          'document_id': documentId,
          'type': cart.type,
          'quantity_delta': cart.type === DocumentType.INBOUND ? line.quantity : -line.quantity,
          'stock_after': updatedStock,
          'created_at': now
        });
        savedLines.push({
          id: lineId,
          documentId,
          productId: line.product.id,
          productName: line.product.name,
          barcode: line.product.barcode,
          unitPriceCents: line.unitPriceCents,
          costPriceSnapshotCents: costSnapshot,
          quantity: line.quantity,
          lineTotalCents: lineTotal
        });
      }

    return {
      id: documentId,
      type: cart.type,
      totalCents: cart.totalCents(),
      note: note.trim(),
      createdAt: now,
      lines: savedLines
    };
  }

  async documents(
    type: DocumentType | null,
    range: DateRange
  ): Promise<StockDocument[]> {
    const clauses: string[] = ['created_at >= ?', 'created_at < ?'];
    const args: relationalStore.ValueType[] = [range.startInclusive, range.endExclusive];
    if (type !== null) {
      clauses.push('type = ?');
      args.push(type);
    }
    const store = await this.database.getStore();
    const resultSet = await store.querySql(
      `SELECT * FROM documents WHERE ${clauses.join(' AND ')} ORDER BY created_at DESC`,
      args
    );
    const documents: StockDocument[] = [];
    try {
      while (resultSet.goToNextRow()) {
        const documentId = resultSet.getLong(resultSet.getColumnIndex('id'));
        const rawType = resultSet.getString(resultSet.getColumnIndex('type'));
        documents.push({
          id: documentId,
          type: rawType === DocumentType.INBOUND ? DocumentType.INBOUND : DocumentType.OUTBOUND,
          totalCents: resultSet.getLong(resultSet.getColumnIndex('total_cents')),
          note: resultSet.getString(resultSet.getColumnIndex('note')),
          createdAt: resultSet.getLong(resultSet.getColumnIndex('created_at')),
          lines: await this.lines(store, documentId)
        });
      }
    } finally {
      resultSet.close();
    }
    return documents;
  }

  private async currentStock(store: relationalStore.RdbStore, productId: number): Promise<number> {
    const resultSet = await store.querySql(
      'SELECT stock_quantity FROM products WHERE id = ? AND active = 1',
      [productId]
    );
    try {
      if (!resultSet.goToFirstRow()) {
        throw new Error('商品不存在或已停用');
      }
      return resultSet.getLong(resultSet.getColumnIndex('stock_quantity'));
    } finally {
      resultSet.close();
    }
  }

  private async lines(
    store: relationalStore.RdbStore,
    documentId: number
  ): Promise<DocumentLine[]> {
    const resultSet = await store.querySql(
      'SELECT * FROM document_lines WHERE document_id = ? ORDER BY id',
      [documentId]
    );
    const lines: DocumentLine[] = [];
    try {
      while (resultSet.goToNextRow()) {
        lines.push({
          id: resultSet.getLong(resultSet.getColumnIndex('id')),
          documentId,
          productId: resultSet.getLong(resultSet.getColumnIndex('product_id')),
          productName: resultSet.getString(resultSet.getColumnIndex('product_name_snapshot')),
          barcode: resultSet.getString(resultSet.getColumnIndex('barcode_snapshot')),
          unitPriceCents: resultSet.getLong(resultSet.getColumnIndex('unit_price_cents')),
          costPriceSnapshotCents: resultSet.getLong(resultSet.getColumnIndex('cost_price_snapshot_cents')),
          quantity: resultSet.getLong(resultSet.getColumnIndex('quantity')),
          lineTotalCents: resultSet.getLong(resultSet.getColumnIndex('line_total_cents'))
        });
      }
    } finally {
      resultSet.close();
    }
    return lines;
  }
}

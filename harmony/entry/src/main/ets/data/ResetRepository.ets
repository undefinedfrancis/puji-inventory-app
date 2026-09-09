import { PujiDatabase } from './PujiDatabase';

export class ResetRepository {
  private readonly database: PujiDatabase;

  constructor(database: PujiDatabase) {
    this.database = database;
  }

  async clearAll(): Promise<void> {
    const store = await this.database.getStore();
    store.beginTransaction();
    try {
      await store.executeSql('DELETE FROM stock_movements');
      await store.executeSql('DELETE FROM document_lines');
      await store.executeSql('DELETE FROM documents');
      await store.executeSql('DELETE FROM products');
      await store.executeSql(
        `DELETE FROM sqlite_sequence
         WHERE name IN ('products','documents','document_lines','stock_movements')`
      );
      store.commit();
    } catch (error) {
      store.rollBack();
      throw new Error(`${error}`);
    }
  }
}

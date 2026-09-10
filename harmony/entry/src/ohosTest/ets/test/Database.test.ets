import { common } from '@kit.AbilityKit';
import { abilityDelegatorRegistry } from '@kit.TestKit';
import { describe, expect, it } from '@ohos/hypium';
import { Product } from '../../../main/ets/model/Models';
import { PujiDatabase } from '../../../main/ets/data/PujiDatabase';
import { ProductRepository } from '../../../main/ets/data/ProductRepository';

async function testContext(): Promise<common.Context> {
  const ability = await abilityDelegatorRegistry.getAbilityDelegator().getCurrentTopAbility();
  return ability.context;
}

function newProduct(barcode: string, name: string): Product {
  return {
    id: 0,
    name,
    barcode,
    category: '测试',
    costPriceCents: 100,
    salePriceCents: 200,
    stockQuantity: 0,
    lowStockThreshold: 2,
    note: '',
    active: true,
    createdAt: 0,
    updatedAt: 0
  };
}

export default function databaseTest(): void {
  describe('PujiDatabase', () => {
    it('createsFourBusinessTables', 0, async () => {
      const context = await testContext();
      const store = await new PujiDatabase(context).getStore();
      const resultSet = await store.querySql(
        `SELECT name FROM sqlite_master
         WHERE type = 'table' AND name IN ('products','documents','document_lines','stock_movements')`
      );
      let count = 0;
      while (resultSet.goToNextRow()) {
        count += 1;
      }
      resultSet.close();
      expect(count).assertEqual(4);
    });

    it('rejectsDuplicateNonEmptyBarcode', 0, async () => {
      const context = await testContext();
      const repository = new ProductRepository(new PujiDatabase(context));
      const barcode = `TEST-${Date.now()}`;
      await repository.save(newProduct(barcode, '条码商品一'));
      let failed = false;
      try {
        await repository.save(newProduct(barcode, '条码商品二'));
      } catch (_) {
        failed = true;
      }
      expect(failed).assertTrue();
    });

    it('allowsMultipleEmptyBarcodes', 0, async () => {
      const context = await testContext();
      const repository = new ProductRepository(new PujiDatabase(context));
      const suffix = Date.now().toString();
      const first = await repository.save(newProduct('', `无条码一-${suffix}`));
      const second = await repository.save(newProduct('', `无条码二-${suffix}`));
      expect(first.id > 0).assertTrue();
      expect(second.id > first.id).assertTrue();
    });
  });
}

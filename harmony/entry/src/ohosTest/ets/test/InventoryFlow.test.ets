import { common } from '@kit.AbilityKit';
import { abilityDelegatorRegistry } from '@kit.TestKit';
import { describe, expect, it } from '@ohos/hypium';
import { InventoryRepository } from '../../../main/ets/data/InventoryRepository';
import { ProductRepository } from '../../../main/ets/data/ProductRepository';
import { PujiDatabase } from '../../../main/ets/data/PujiDatabase';
import { monthRange } from '../../../main/ets/domain/Periods';
import { DocumentType } from '../../../main/ets/model/Models';
import type { Product } from '../../../main/ets/model/Models';
import { PujiStore, ScanOutcome } from '../../../main/ets/state/PujiStore';

function inboundProduct(barcode: string): Product {
  return {
    id: 0,
    name: '数量回归测试商品',
    barcode,
    category: '测试',
    costPriceCents: 250,
    salePriceCents: 400,
    stockQuantity: 0,
    lowStockThreshold: 2,
    note: '',
    active: true,
    createdAt: 0,
    updatedAt: 0
  };
}

export default function inventoryFlowTest(): void {
  describe('InventoryFlow', () => {
    it('persistsExactUnknownInboundQuantity', 0, async () => {
      const context = abilityDelegatorRegistry.getAbilityDelegator().getAppContext() as common.Context;
      const database = new PujiDatabase(context);
      const products = new ProductRepository(database);
      const inventory = new InventoryRepository(database);
      const appStore = new PujiStore();
      await appStore.initialize(context);
      const barcode = `FLOW-${Date.now()}`;
      const outcome = await appStore.scanBarcode(barcode);
      expect(outcome).assertEqual(ScanOutcome.UNKNOWN);
      await appStore.saveUnknownInbound(inboundProduct(barcode), 5);

      const reloaded = await products.findByBarcode(barcode);
      expect(reloaded?.stockQuantity).assertEqual(5);
      const documents = await inventory.documents(DocumentType.INBOUND, monthRange(Date.now()));
      const matching = documents.find((document) => document.lines.some((line) => line.barcode === barcode));
      expect(matching?.lines[0].quantity).assertEqual(5);

      const rollbackBarcode = `ROLLBACK-${Date.now()}`;
      let failed = false;
      try {
        await inventory.createProductAndCommitInbound(inboundProduct(rollbackBarcode), 0);
      } catch (error) {
        failed = true;
      }
      expect(failed).assertTrue();
      expect(await products.findByBarcode(rollbackBarcode)).assertNull();
    });
  });
}

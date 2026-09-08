import { describe, expect, it } from '@ohos/hypium';
import { nextStock } from '../main/ets/domain/InventoryRules';
import { DocumentType } from '../main/ets/model/Models';

export default function inventoryRulesTest(): void {
  describe('InventoryRules', () => {
    it('keepsExactInboundQuantity', 0, () => {
      expect(nextStock(DocumentType.INBOUND, 0, 5)).assertEqual(5);
    });

    it('rejectsInsufficientOutboundStock', 0, () => {
      expect(nextStock(DocumentType.OUTBOUND, 2, 3)).assertNull();
    });
  });
}

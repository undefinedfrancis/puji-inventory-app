import { describe, expect, it } from '@ohos/hypium';
import { Cart } from '../main/ets/domain/Cart';
import { DocumentType, Product } from '../main/ets/model/Models';

const cola: Product = {
  id: 1,
  name: '可口可乐 500ml',
  barcode: '6901234567890',
  category: '饮料',
  costPriceCents: 220,
  salePriceCents: 350,
  stockQuantity: 12,
  lowStockThreshold: 5,
  note: '',
  active: true,
  createdAt: 0,
  updatedAt: 0
};

export default function cartTest(): void {
  describe('Cart', () => {
    it('addsExactScannedInboundQuantity', 0, () => {
      const cart = Cart.empty(DocumentType.INBOUND).add(cola, 5);
      expect(cart.lines[0].quantity).assertEqual(5);
      expect(cart.totalCents()).assertEqual(1100);
    });

    it('mergesRepeatedProduct', 0, () => {
      const cart = Cart.empty(DocumentType.OUTBOUND).add(cola).add(cola);
      expect(cart.lines.length).assertEqual(1);
      expect(cart.lines[0].quantity).assertEqual(2);
    });

    it('removesLastUnit', 0, () => {
      const cart = Cart.empty(DocumentType.OUTBOUND).add(cola).decrement(cola.id);
      expect(cart.lines.length).assertEqual(0);
    });
  });
}

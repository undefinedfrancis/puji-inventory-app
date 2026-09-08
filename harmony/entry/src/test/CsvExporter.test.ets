import { describe, expect, it } from '@ohos/hypium';
import { escapeCsv, exportProducts } from '../main/ets/export/CsvExporter';
import { Product } from '../main/ets/model/Models';

export default function csvExporterTest(): void {
  describe('CsvExporter', () => {
    it('escapesSpecialCharacters', 0, () => {
      expect(escapeCsv('饮料,冷藏')).assertEqual('"饮料,冷藏"');
      expect(escapeCsv('大瓶"装')).assertEqual('"大瓶""装"');
    });

    it('exportsUtf8BomAndStock', 0, () => {
      const products: Product[] = [{
        id: 1,
        name: '矿泉水',
        barcode: '6902',
        category: '饮料',
        costPriceCents: 100,
        salePriceCents: 200,
        stockQuantity: 5,
        lowStockThreshold: 2,
        note: '',
        active: true,
        createdAt: 0,
        updatedAt: 0
      }];
      expect(exportProducts(products).startsWith('\uFEFF')).assertTrue();
    });
  });
}

import { describe, expect, it } from '@ohos/hypium';
import { formatMoney, lineTotal, parseMoneyToCents } from '../main/ets/domain/Money';

export default function moneyTest(): void {
  describe('Money', () => {
    it('formatsIntegerCents', 0, () => {
      expect(formatMoney(12345)).assertEqual('¥123.45');
    });

    it('formatsNegativeCents', 0, () => {
      expect(formatMoney(-30)).assertEqual('-¥0.30');
    });

    it('parsesTwoDecimals', 0, () => {
      expect(parseMoneyToCents('12.30')).assertEqual(1230);
    });

    it('parsesWholeAmount', 0, () => {
      expect(parseMoneyToCents(' 8 ')).assertEqual(800);
    });

    it('rejectsInvalidMoney', 0, () => {
      expect(parseMoneyToCents('12.345')).assertNull();
    });

    it('rejectsNegativeMoney', 0, () => {
      expect(parseMoneyToCents('-1')).assertNull();
    });

    it('calculatesLineTotalInCents', 0, () => {
      expect(lineTotal(399, 5)).assertEqual(1995);
    });
  });
}

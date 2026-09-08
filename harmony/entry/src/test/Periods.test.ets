import { describe, expect, it } from '@ohos/hypium';
import { customDateRange, monthRange, quarterRange, yearRange } from '../main/ets/domain/Periods';

export default function periodsTest(): void {
  describe('Periods', () => {
    it('buildsJanuaryRange', 0, () => {
      const range = monthRange(new Date(2026, 0, 18, 12).getTime());
      expect(range.startInclusive).assertEqual(new Date(2026, 0, 1).getTime());
      expect(range.endExclusive).assertEqual(new Date(2026, 1, 1).getTime());
    });

    it('buildsSecondQuarterRange', 0, () => {
      const range = quarterRange(new Date(2026, 4, 8, 12).getTime());
      expect(range.startInclusive).assertEqual(new Date(2026, 3, 1).getTime());
      expect(range.endExclusive).assertEqual(new Date(2026, 6, 1).getTime());
    });

    it('includesLeapDayInFebruary', 0, () => {
      const range = monthRange(new Date(2028, 1, 29, 12).getTime());
      expect(range.startInclusive).assertEqual(new Date(2028, 1, 1).getTime());
      expect(range.endExclusive).assertEqual(new Date(2028, 2, 1).getTime());
    });

    it('buildsYearRangeAcrossBoundary', 0, () => {
      const range = yearRange(new Date(2026, 11, 31, 23, 59).getTime());
      expect(range.startInclusive).assertEqual(new Date(2026, 0, 1).getTime());
      expect(range.endExclusive).assertEqual(new Date(2027, 0, 1).getTime());
    });

    it('normalizesInclusiveCustomEndDay', 0, () => {
      const range = customDateRange(
        new Date(2026, 8, 2, 15).getTime(),
        new Date(2026, 8, 5, 2).getTime()
      );
      expect(range?.startInclusive).assertEqual(new Date(2026, 8, 2).getTime());
      expect(range?.endExclusive).assertEqual(new Date(2026, 8, 6).getTime());
    });

    it('rejectsReversedCustomRange', 0, () => {
      expect(customDateRange(
        new Date(2026, 8, 5).getTime(),
        new Date(2026, 8, 2).getTime()
      )).assertNull();
    });
  });
}

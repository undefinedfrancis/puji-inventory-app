import { describe, expect, it } from '@ohos/hypium';
import {
  canMovePeriodForward,
  customDateRange,
  monthRange,
  parseLocalDate,
  quarterRange,
  shiftPeriodAnchor,
  yearRange
} from '../main/ets/domain/Periods';
import { PeriodKind } from '../main/ets/model/Models';

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

    it('parsesStrictLocalDate', 0, () => {
      expect(parseLocalDate('2026-09-08')).assertEqual(new Date(2026, 8, 8).getTime());
      expect(parseLocalDate('2026-02-30')).assertNull();
      expect(parseLocalDate('2026-9-8')).assertNull();
    });

    it('movesSafelyFromMonthEnd', 0, () => {
      const shifted = shiftPeriodAnchor(
        new Date(2026, 2, 31, 12).getTime(),
        PeriodKind.MONTH,
        -1
      );
      expect(shifted).assertEqual(new Date(2026, 1, 1).getTime());
    });
  });
}

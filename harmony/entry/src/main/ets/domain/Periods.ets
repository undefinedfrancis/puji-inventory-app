import type { DateRange } from '../model/Models';

export function monthRange(anchorMs: number): DateRange {
  const anchor = new Date(anchorMs);
  return {
    startInclusive: new Date(anchor.getFullYear(), anchor.getMonth(), 1).getTime(),
    endExclusive: new Date(anchor.getFullYear(), anchor.getMonth() + 1, 1).getTime()
  };
}

export function quarterRange(anchorMs: number): DateRange {
  const anchor = new Date(anchorMs);
  const firstMonth = Math.floor(anchor.getMonth() / 3) * 3;
  return {
    startInclusive: new Date(anchor.getFullYear(), firstMonth, 1).getTime(),
    endExclusive: new Date(anchor.getFullYear(), firstMonth + 3, 1).getTime()
  };
}

export function yearRange(anchorMs: number): DateRange {
  const anchor = new Date(anchorMs);
  return {
    startInclusive: new Date(anchor.getFullYear(), 0, 1).getTime(),
    endExclusive: new Date(anchor.getFullYear() + 1, 0, 1).getTime()
  };
}

export function customDateRange(startMs: number, endMs: number): DateRange | null {
  const start = new Date(startMs);
  const end = new Date(endMs);
  const startInclusive = new Date(start.getFullYear(), start.getMonth(), start.getDate()).getTime();
  const endInclusive = new Date(end.getFullYear(), end.getMonth(), end.getDate()).getTime();
  if (startInclusive > endInclusive) {
    return null;
  }
  return {
    startInclusive,
    endExclusive: new Date(end.getFullYear(), end.getMonth(), end.getDate() + 1).getTime()
  };
}

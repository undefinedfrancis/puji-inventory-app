import type { DateRange } from '../model/Models';
import { PeriodKind } from '../model/Models';

export function shiftPeriodAnchor(anchorMs: number, kind: PeriodKind, offset: number): number {
  const anchor = new Date(anchorMs);
  if (kind === PeriodKind.QUARTER) {
    const quarterStartMonth = Math.floor(anchor.getMonth() / 3) * 3;
    return new Date(anchor.getFullYear(), quarterStartMonth + offset * 3, 1).getTime();
  }
  if (kind === PeriodKind.YEAR) {
    return new Date(anchor.getFullYear() + offset, 0, 1).getTime();
  }
  return new Date(anchor.getFullYear(), anchor.getMonth() + offset, 1).getTime();
}

export function canMovePeriodForward(anchorMs: number, kind: PeriodKind, nowMs: number): boolean {
  return shiftPeriodAnchor(anchorMs, kind, 1) <= nowMs;
}

export function parseLocalDate(raw: string): number | null {
  const trimmed = raw.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    return null;
  }
  const parts = trimmed.split('-');
  const year = Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null;
  }
  return date.getTime();
}

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

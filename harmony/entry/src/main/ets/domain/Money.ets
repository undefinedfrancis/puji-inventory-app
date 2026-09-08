export function formatMoney(cents: number): string {
  const rounded = Math.trunc(cents);
  const absolute = Math.abs(rounded);
  const yuan = Math.floor(absolute / 100);
  const fraction = (absolute % 100).toString().padStart(2, '0');
  const sign = rounded < 0 ? '-' : '';
  return `${sign}¥${yuan}.${fraction}`;
}

export function parseMoneyToCents(raw: string): number | null {
  const normalized = raw.trim();
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) {
    return null;
  }

  const parts = normalized.split('.');
  const yuan = Number(parts[0]);
  const fraction = parts.length === 1 ? 0 : Number(parts[1].padEnd(2, '0'));
  const cents = yuan * 100 + fraction;
  return Number.isSafeInteger(cents) ? cents : null;
}

export function lineTotal(unitPriceCents: number, quantity: number): number {
  return Math.trunc(unitPriceCents) * Math.trunc(quantity);
}

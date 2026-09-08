import { DocumentType } from '../model/Models';
import type { Cart, CartLine } from './Cart';

export function nextStock(
  type: DocumentType,
  currentStock: number,
  quantity: number
): number | null {
  const current = Math.trunc(currentStock);
  const delta = Math.trunc(quantity);
  if (current < 0 || delta <= 0) {
    return null;
  }
  if (type === DocumentType.INBOUND) {
    return current + delta;
  }
  return current >= delta ? current - delta : null;
}

export function validateCart(cart: Cart): string | null {
  if (cart.lines.length === 0) {
    return '请先添加商品';
  }
  for (let index = 0; index < cart.lines.length; index += 1) {
    const line: CartLine = cart.lines[index];
    if (!line.product.active) {
      return `商品“${line.product.name}”已停用`;
    }
    if (line.quantity <= 0) {
      return `商品“${line.product.name}”数量无效`;
    }
    if (cart.type === DocumentType.OUTBOUND && line.quantity > line.product.stockQuantity) {
      return `商品“${line.product.name}”库存不足`;
    }
  }
  return null;
}

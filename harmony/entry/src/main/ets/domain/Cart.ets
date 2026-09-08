import { DocumentType } from '../model/Models';
import type { Product } from '../model/Models';

export interface CartLine {
  product: Product;
  quantity: number;
  unitPriceCents: number;
}

export class Cart {
  readonly type: DocumentType;
  readonly lines: CartLine[];
  readonly note: string;

  constructor(type: DocumentType, lines: CartLine[] = [], note: string = '') {
    this.type = type;
    this.lines = lines;
    this.note = note;
  }

  static empty(type: DocumentType): Cart {
    return new Cart(type);
  }

  add(product: Product, quantity: number = 1): Cart {
    const normalizedQuantity = Math.trunc(quantity);
    if (normalizedQuantity <= 0) {
      throw new Error('数量必须大于 0');
    }

    const existingIndex = this.lines.findIndex((line: CartLine) => line.product.id === product.id);
    if (existingIndex < 0) {
      const unitPriceCents = this.type === DocumentType.INBOUND
        ? product.costPriceCents
        : product.salePriceCents;
      return new Cart(this.type, [
        ...this.lines,
        { product, quantity: normalizedQuantity, unitPriceCents }
      ], this.note);
    }

    const nextLines = this.lines.map((line: CartLine, index: number) => index === existingIndex
      ? { product: line.product, quantity: line.quantity + normalizedQuantity, unitPriceCents: line.unitPriceCents }
      : line);
    return new Cart(this.type, nextLines, this.note);
  }

  increment(productId: number): Cart {
    const nextLines = this.lines.map((line: CartLine) => line.product.id === productId
      ? { product: line.product, quantity: line.quantity + 1, unitPriceCents: line.unitPriceCents }
      : line);
    return new Cart(this.type, nextLines, this.note);
  }

  decrement(productId: number): Cart {
    const nextLines: CartLine[] = [];
    this.lines.forEach((line: CartLine) => {
      if (line.product.id !== productId) {
        nextLines.push(line);
      } else if (line.quantity > 1) {
        nextLines.push({
          product: line.product,
          quantity: line.quantity - 1,
          unitPriceCents: line.unitPriceCents
        });
      }
    });
    return new Cart(this.type, nextLines, this.note);
  }

  updateUnitPrice(productId: number, cents: number): Cart {
    const normalizedCents = Math.trunc(cents);
    if (normalizedCents < 0) {
      throw new Error('价格不能小于 0');
    }
    const nextLines = this.lines.map((line: CartLine) => line.product.id === productId
      ? { product: line.product, quantity: line.quantity, unitPriceCents: normalizedCents }
      : line);
    return new Cart(this.type, nextLines, this.note);
  }

  remove(productId: number): Cart {
    return new Cart(
      this.type,
      this.lines.filter((line: CartLine) => line.product.id !== productId),
      this.note
    );
  }

  totalCents(): number {
    return this.lines.reduce(
      (total: number, line: CartLine) => total + line.unitPriceCents * line.quantity,
      0
    );
  }

  totalQuantity(): number {
    return this.lines.reduce((total: number, line: CartLine) => total + line.quantity, 0);
  }
}

export class ScanGate {
  private locked: boolean = false;

  accept(raw: string): string | null {
    if (this.locked) {
      return null;
    }
    const barcode = raw.trim();
    if (barcode.length === 0) {
      return null;
    }
    this.locked = true;
    return barcode;
  }

  complete(): void {
    this.locked = false;
  }

  cancel(): void {
    this.locked = false;
  }

  isLocked(): boolean {
    return this.locked;
  }
}

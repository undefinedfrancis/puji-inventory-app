export class HoldToConfirm {
  private readonly durationMs: number;
  private startedAt: number | null = null;
  private confirmed: boolean = false;

  constructor(durationMs: number = 2000) {
    this.durationMs = Math.max(1, Math.trunc(durationMs));
  }

  press(nowMs: number): void {
    this.startedAt = nowMs;
    this.confirmed = false;
  }

  release(): void {
    this.startedAt = null;
    this.confirmed = false;
  }

  tick(nowMs: number): boolean {
    if (this.startedAt === null || this.confirmed) {
      return false;
    }
    if (nowMs - this.startedAt < this.durationMs) {
      return false;
    }
    this.confirmed = true;
    return true;
  }

  progress(nowMs: number): number {
    if (this.startedAt === null) {
      return 0;
    }
    return Math.min(1, Math.max(0, (nowMs - this.startedAt) / this.durationMs));
  }
}

export class LatestRequestGate {
  private version: number = 0;

  begin(): number {
    this.version += 1;
    return this.version;
  }

  isLatest(requestVersion: number): boolean {
    return requestVersion === this.version;
  }
}

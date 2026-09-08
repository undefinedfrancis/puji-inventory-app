import { describe, expect, it } from '@ohos/hypium';
import { ScanGate } from '../main/ets/domain/ScanGate';

export default function scanGateTest(): void {
  describe('ScanGate', () => {
    it('rejectsDuplicateCallbackWhileLocked', 0, () => {
      const gate = new ScanGate();
      expect(gate.accept('6901')).assertEqual('6901');
      expect(gate.accept('6901')).assertNull();
    });

    it('unlocksAfterComplete', 0, () => {
      const gate = new ScanGate();
      gate.accept('6901');
      gate.complete();
      expect(gate.accept('6902')).assertEqual('6902');
    });
  });
}

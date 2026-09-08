import cartTest from './Cart.test';
import moneyTest from './Money.test';
import periodsTest from './Periods.test';
import scanGateTest from './ScanGate.test';

export default function testsuite(): void {
  cartTest();
  moneyTest();
  periodsTest();
  scanGateTest();
}

import businessSummaryTest from './BusinessSummary.test';
import cartTest from './Cart.test';
import csvExporterTest from './CsvExporter.test';
import moneyTest from './Money.test';
import periodsTest from './Periods.test';
import scanGateTest from './ScanGate.test';

export default function testsuite(): void {
  businessSummaryTest();
  cartTest();
  csvExporterTest();
  moneyTest();
  periodsTest();
  scanGateTest();
}

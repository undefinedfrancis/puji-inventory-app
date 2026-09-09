import databaseTest from './Database.test';
import inventoryFlowTest from './InventoryFlow.test';

export default function testsuite(): void {
  databaseTest();
  inventoryFlowTest();
}

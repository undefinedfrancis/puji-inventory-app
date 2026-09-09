import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('clears the manual barcode after a newly scanned product is saved or cancelled', async () => {
  const source = await readFile(
    new URL('../entry/src/main/ets/pages/ScannerPage.ets', import.meta.url),
    'utf8'
  );

  assert.match(source, /saveUnknownInbound\(product, quantity\)[\s\S]*manualBarcode = ''/);
  assert.match(source, /cancelPendingScan\(\)[\s\S]*manualBarcode = ''/);
});

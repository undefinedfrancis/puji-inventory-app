import { common } from '@kit.AbilityKit';
import { scanBarcode, scanCore } from '@kit.ScanKit';

export interface BarcodeScanResponse {
  value: string;
  errorMessage: string;
}

export class BarcodeScanner {
  async scan(context: common.Context): Promise<BarcodeScanResponse> {
    try {
      const result = await scanBarcode.startScanForResult(context, {
        scanTypes: [scanCore.ScanType.ONE_D_CODE],
        enableMultiMode: false,
        enableAlbum: true
      });
      return {
        value: result.originalValue.trim(),
        errorMessage: ''
      };
    } catch (error) {
      const message = `${error}`;
      if (message.includes('1000500002')) {
        return { value: '', errorMessage: '' };
      }
      return { value: '', errorMessage: '无法打开扫码，请稍后重试或手动输入条码' };
    }
  }
}

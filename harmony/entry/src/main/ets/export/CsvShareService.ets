import { common } from '@kit.AbilityKit';
import { fileIo, fileUri } from '@kit.CoreFileKit';
import { systemShare } from '@kit.ShareKit';

export class CsvShareService {
  async share(context: common.UIAbilityContext, fileName: string, content: string): Promise<void> {
    const path = `${context.cacheDir}/${fileName}`;
    const file = fileIo.openSync(
      path,
      fileIo.OpenMode.CREATE | fileIo.OpenMode.WRITE_ONLY | fileIo.OpenMode.TRUNC
    );
    try {
      fileIo.writeSync(file.fd, content);
    } finally {
      fileIo.closeSync(file);
    }

    const data = new systemShare.SharedData({
      utd: 'general.comma-separated-values-text',
      uri: fileUri.getUriFromPath(path),
      title: fileName,
      label: 'FILE',
      description: '铺记导出数据'
    });
    const controller = new systemShare.ShareController(data);
    await controller.show(context, {});
  }
}

import { common } from '@kit.AbilityKit';
import { EmptyState } from '../components/CommonComponents';
import { formatMoney } from '../domain/Money';
import { parseLocalDate } from '../domain/Periods';
import { CsvShareService } from '../export/CsvShareService';
import { DocumentType } from '../model/Models';
import type { StockDocument } from '../model/Models';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

function twoDigits(value: number): string {
  return value.toString().padStart(2, '0');
}

function dateText(timestamp: number): string {
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${twoDigits(date.getMonth() + 1)}-${twoDigits(date.getDate())}`;
}

function documentTime(timestamp: number): string {
  const date = new Date(timestamp);
  return `${dateText(timestamp)} ${twoDigits(date.getHours())}:${twoDigits(date.getMinutes())}`;
}

@Component
export struct LedgerPage {
  @ObjectLink store: PujiStore;
  onBack: () => void = () => {};

  @State selectedType: string = '全部';
  @State startDateText: string = '';
  @State endDateText: string = '';
  @State exporting: boolean = false;

  private readonly csvShareService: CsvShareService = new CsvShareService();

  aboutToAppear(): void {
    const anchor = new Date(this.store.ledgerAnchorMs);
    this.startDateText = dateText(new Date(anchor.getFullYear(), anchor.getMonth(), 1).getTime());
    this.endDateText = dateText(Date.now());
    this.store.loadLedger();
  }

  private typeFilter(): DocumentType | null {
    if (this.selectedType === '入货') {
      return DocumentType.INBOUND;
    }
    if (this.selectedType === '出货') {
      return DocumentType.OUTBOUND;
    }
    return null;
  }

  private monthLabel(): string {
    const date = new Date(this.store.ledgerAnchorMs);
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`;
  }

  private selectType(label: string): void {
    this.selectedType = label;
    this.store.loadLedger(this.typeFilter());
  }

  private async shareMonth(): Promise<void> {
    if (this.exporting) {
      return;
    }
    this.exporting = true;
    try {
      const content = await this.store.monthDocumentsCsv();
      const date = new Date(this.store.ledgerAnchorMs);
      const fileName = `铺记流水-${date.getFullYear()}-${twoDigits(date.getMonth() + 1)}.csv`;
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await this.csvShareService.share(context, fileName, content);
      this.store.message = '月流水已生成，请选择保存或分享位置';
    } catch (error) {
      this.store.message = `导出失败：${error}`;
    } finally {
      this.exporting = false;
    }
  }

  private async shareCustom(): Promise<void> {
    if (this.exporting) {
      return;
    }
    const startMs = parseLocalDate(this.startDateText);
    const endMs = parseLocalDate(this.endDateText);
    if (startMs === null || endMs === null || startMs > endMs) {
      this.store.message = '请输入正确的起止日期，例如 2026-09-01';
      return;
    }
    this.exporting = true;
    try {
      const content = await this.store.customDocumentsCsv(startMs, endMs);
      if (content === null) {
        this.store.message = '结束日期不能早于开始日期';
        return;
      }
      const fileName = `铺记流水-${this.startDateText}-${this.endDateText}.csv`;
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await this.csvShareService.share(context, fileName, content);
      this.store.message = '自定义流水已生成，请选择保存或分享位置';
    } catch (error) {
      this.store.message = `导出失败：${error}`;
    } finally {
      this.exporting = false;
    }
  }

  build() {
    Column({ space: 12 }) {
      Row({ space: 8 }) {
        Button('‹')
          .width(48)
          .height(48)
          .fontSize(25)
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(PUJI_COLORS.surface)
          .onClick(this.onBack)
        Column({ space: 2 }) {
          Text('流水明细')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(PUJI_COLORS.ink)
          Text('按月份查看，支持月度和自定义导出')
            .fontSize(12)
            .fontColor(PUJI_COLORS.inkMuted)
        }
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')

      Row() {
        Button('‹')
          .width(48)
          .height(48)
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(PUJI_COLORS.surface)
          .onClick(() => {
            this.store.moveLedgerMonth(-1, this.typeFilter());
          })
        Blank()
        Text(this.monthLabel())
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(PUJI_COLORS.ink)
        Blank()
        Button('›')
          .width(48)
          .height(48)
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(PUJI_COLORS.surface)
          .onClick(() => {
            this.store.moveLedgerMonth(1, this.typeFilter());
          })
      }

      Row({ space: 7 }) {
        ForEach(['全部', '入货', '出货'], (label: string) => {
          Button(label)
            .layoutWeight(1)
            .height(46)
            .fontColor(this.selectedType === label ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.selectedType === label ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(15)
            .onClick(() => this.selectType(label))
        }, (label: string) => label)
      }

      if (this.store.ledgerDocuments.length === 0) {
        EmptyState({ title: '本月暂无流水', message: '完成入货或出货登记后，流水会自动出现在这里。' })
      } else {
        List({ space: 8 }) {
          ForEach(this.store.ledgerDocuments, (document: StockDocument) => {
            ListItem() {
              Column({ space: 7 }) {
                Row() {
                  Text(document.type === DocumentType.INBOUND ? '入货' : '出货')
                    .fontSize(12)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(document.type === DocumentType.INBOUND ? PUJI_COLORS.primary : PUJI_COLORS.scanAccent)
                    .padding({ top: 4, right: 8, bottom: 4, left: 8 })
                    .backgroundColor(document.type === DocumentType.INBOUND ? PUJI_COLORS.successSoft : PUJI_COLORS.warningSoft)
                    .borderRadius(8)
                  Blank()
                  Text(formatMoney(document.totalCents))
                    .fontSize(17)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(PUJI_COLORS.ink)
                }
                Text(document.lines.map((line) => `${line.productName} × ${line.quantity}`).join('、'))
                  .fontSize(13)
                  .fontColor(PUJI_COLORS.ink)
                  .maxLines(2)
                Text(`${documentTime(document.createdAt)} · 单号 ${document.id}`)
                  .fontSize(11)
                  .fontColor(PUJI_COLORS.inkMuted)
              }
              .padding(14)
              .width('100%')
              .alignItems(HorizontalAlign.Start)
              .backgroundColor(PUJI_COLORS.surface)
              .borderRadius(17)
              .border({ width: 1, color: PUJI_COLORS.line })
            }
          }, (document: StockDocument) => document.id.toString())
        }
        .layoutWeight(1)
        .width('100%')
        .scrollBar(BarState.Off)
      }

      Column({ space: 8 }) {
        Button(this.exporting ? '正在生成…' : '导出本月流水')
          .height(50)
          .width('100%')
          .fontColor(Color.White)
          .backgroundColor(PUJI_COLORS.primary)
          .borderRadius(16)
          .enabled(!this.exporting)
          .onClick(() => this.shareMonth())
        Row({ space: 8 }) {
          TextInput({ placeholder: '开始 YYYY-MM-DD', text: this.startDateText })
            .layoutWeight(1)
            .height(48)
            .backgroundColor(PUJI_COLORS.background)
            .borderRadius(14)
            .onChange((value: string) => this.startDateText = value)
          TextInput({ placeholder: '结束 YYYY-MM-DD', text: this.endDateText })
            .layoutWeight(1)
            .height(48)
            .backgroundColor(PUJI_COLORS.background)
            .borderRadius(14)
            .onChange((value: string) => this.endDateText = value)
        }
        Button('按自定义日期导出')
          .height(50)
          .width('100%')
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(PUJI_COLORS.successSoft)
          .borderRadius(16)
          .enabled(!this.exporting)
          .onClick(() => this.shareCustom())
      }
      .padding(12)
      .width('100%')
      .backgroundColor(PUJI_COLORS.surface)
      .borderRadius(PUJI_LAYOUT.cardRadius)

      if (this.store.message.length > 0) {
        Text(this.store.message)
          .fontSize(12)
          .fontColor(PUJI_COLORS.inkMuted)
          .width('100%')
      }
    }
    .padding({ top: 18, right: PUJI_LAYOUT.pagePadding, bottom: 20, left: PUJI_LAYOUT.pagePadding })
    .backgroundColor(PUJI_COLORS.background)
    .width('100%')
    .height('100%')
  }
}

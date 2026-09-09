import { common } from '@kit.AbilityKit';
import { EmptyState, PageHeader } from '../components/CommonComponents';
import { ProductEditor } from '../components/ProductEditor';
import { formatMoney } from '../domain/Money';
import type { CartLine } from '../domain/Cart';
import { DocumentType } from '../model/Models';
import { BarcodeScanner } from '../scanner/BarcodeScanner';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct ScannerPage {
  @ObjectLink store: PujiStore;
  @State manualBarcode: string = '';
  @State cameraBusy: boolean = false;

  private readonly barcodeScanner: BarcodeScanner = new BarcodeScanner();

  private async openCamera(): Promise<void> {
    if (this.cameraBusy || this.store.busy || this.store.pendingBarcode.length > 0) {
      return;
    }
    this.cameraBusy = true;
    try {
      const context = this.getUIContext().getHostContext() as common.Context;
      const response = await this.barcodeScanner.scan(context);
      if (response.errorMessage.length > 0) {
        this.store.message = response.errorMessage;
        return;
      }
      if (response.value.length > 0) {
        await this.store.scanBarcode(response.value);
      }
    } finally {
      this.cameraBusy = false;
    }
  }

  build() {
    Scroll() {
      Column({ space: 14 }) {
        PageHeader({ title: '扫码登记', subtitle: '识别成功后会暂停相机，保存或取消后才继续扫描。' })

        Row({ space: 8 }) {
          Button('入货')
            .layoutWeight(1)
            .height(50)
            .fontColor(this.store.documentType === DocumentType.INBOUND ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.store.documentType === DocumentType.INBOUND ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(16)
            .onClick(() => {
              this.store.setDocumentType(DocumentType.INBOUND);
            })
          Button('出货')
            .layoutWeight(1)
            .height(50)
            .fontColor(this.store.documentType === DocumentType.INBOUND ? PUJI_COLORS.ink : Color.White)
            .backgroundColor(this.store.documentType === DocumentType.INBOUND ? PUJI_COLORS.surface : PUJI_COLORS.scanAccent)
            .borderRadius(16)
            .onClick(() => {
              this.store.setDocumentType(DocumentType.OUTBOUND);
            })
        }

        Stack() {
          Column({ space: 10 }) {
            Text('▣')
              .fontSize(62)
              .fontColor(Color.White)
            Text('将商品条码放入框内')
              .fontSize(15)
              .fontColor(Color.White)
            Text(this.store.pendingBarcode.length > 0 ? '请先完成当前商品信息' : '扫描成功后自动停止')
              .fontSize(12)
              .fontColor('#DDECE3')
            Button(this.cameraBusy ? '正在打开相机…' : '打开相机扫码')
              .height(48)
              .fontWeight(FontWeight.Bold)
              .fontColor(PUJI_COLORS.primaryDark)
              .backgroundColor(Color.White)
              .borderRadius(16)
              .enabled(!this.cameraBusy && !this.store.busy && this.store.pendingBarcode.length === 0)
              .onClick(() => {
                this.openCamera();
              })
          }
          .justifyContent(FlexAlign.Center)
          .width('100%')
          .height('100%')
        }
        .height(240)
        .width('100%')
        .backgroundColor(PUJI_COLORS.primaryDark)
        .borderRadius(26)

        Row({ space: 8 }) {
          TextInput({ placeholder: '也可手动输入条码', text: this.manualBarcode })
            .layoutWeight(1)
            .height(52)
            .backgroundColor(PUJI_COLORS.surface)
            .borderRadius(16)
            .onChange((value: string) => {
              this.manualBarcode = value;
            })
          Button('添加')
            .height(52)
            .fontColor(Color.White)
            .backgroundColor(PUJI_COLORS.scanAccent)
            .borderRadius(16)
            .onClick(() => {
              this.store.scanBarcode(this.manualBarcode).then(() => {
                if (this.store.pendingBarcode.length === 0) {
                  this.manualBarcode = '';
                }
              });
            })
        }

        if (this.store.pendingBarcode.length > 0) {
          ProductEditor({
            barcode: this.store.pendingBarcode,
            busy: this.store.busy,
            onSave: (product, quantity: number) => {
              this.store.saveUnknownInbound(product, quantity);
            },
            onCancel: () => {
              this.store.cancelPendingScan();
            }
          })
        } else if (this.store.cart.lines.length === 0) {
          EmptyState({ title: '登记清单为空', message: '扫描或输入条码后，商品会加入本次登记。' })
        } else {
          Column({ space: 8 }) {
            ForEach(this.store.cart.lines, (line: CartLine) => {
              Row({ space: 10 }) {
                Column({ space: 4 }) {
                  Text(line.product.name)
                    .fontSize(15)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(PUJI_COLORS.ink)
                    .maxLines(1)
                  Text(`${formatMoney(line.unitPriceCents)} × ${line.quantity}`)
                    .fontSize(12)
                    .fontColor(PUJI_COLORS.inkMuted)
                }
                .layoutWeight(1)
                .alignItems(HorizontalAlign.Start)
                Button('－')
                  .width(48)
                  .height(48)
                  .fontColor(PUJI_COLORS.primary)
                  .backgroundColor(PUJI_COLORS.successSoft)
                  .onClick(() => {
                    this.store.decrement(line.product.id);
                  })
                Text(line.quantity.toString())
                  .fontSize(16)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(PUJI_COLORS.ink)
                Button('＋')
                  .width(48)
                  .height(48)
                  .fontColor(Color.White)
                  .backgroundColor(PUJI_COLORS.primary)
                  .onClick(() => {
                    this.store.increment(line.product.id);
                  })
              }
              .padding(12)
              .width('100%')
              .backgroundColor(PUJI_COLORS.surface)
              .borderRadius(17)
            }, (line: CartLine) => line.product.id.toString())
          }
          .width('100%')
        }

        if (this.store.message.length > 0) {
          Text(this.store.message)
            .fontSize(13)
            .fontColor(PUJI_COLORS.inkMuted)
            .width('100%')
        }

        Button(this.store.documentType === DocumentType.INBOUND
          ? `确认入货 · ${formatMoney(this.store.cart.totalCents())}`
          : `确认出货 · ${formatMoney(this.store.cart.totalCents())}`)
          .height(56)
          .width('100%')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
          .backgroundColor(this.store.documentType === DocumentType.INBOUND ? PUJI_COLORS.primary : PUJI_COLORS.scanAccent)
          .borderRadius(18)
          .enabled(this.store.cart.lines.length > 0 && !this.store.busy && this.store.pendingBarcode.length === 0)
          .onClick(() => {
            this.store.commitCart();
          })
      }
      .padding({ top: 22, right: PUJI_LAYOUT.pagePadding, bottom: 30, left: PUJI_LAYOUT.pagePadding })
      .width('100%')
    }
    .scrollBar(BarState.Off)
    .backgroundColor(PUJI_COLORS.background)
    .width('100%')
    .height('100%')
  }
}

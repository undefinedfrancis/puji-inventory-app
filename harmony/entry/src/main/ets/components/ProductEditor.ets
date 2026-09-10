import { parseMoneyToCents } from '../domain/Money';
import type { Product } from '../model/Models';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct ProductEditor {
  @Prop barcode: string = '';
  @Prop busy: boolean = false;
  onSave: (product: Product, quantity: number) => void = () => {};
  onCancel: () => void = () => {};

  @State name: string = '';
  @State category: string = '未分类';
  @State costText: string = '';
  @State saleText: string = '';
  @State quantityText: string = '1';
  @State error: string = '';

  private submit(): void {
    const costPrice = parseMoneyToCents(this.costText);
    const salePrice = parseMoneyToCents(this.saleText);
    const quantity = Number(this.quantityText.trim());
    if (this.name.trim().length === 0) {
      this.error = '请输入商品名称';
      return;
    }
    if (costPrice === null || salePrice === null) {
      this.error = '价格最多保留两位小数';
      return;
    }
    if (!Number.isInteger(quantity) || quantity < 1) {
      this.error = '本次入货数量至少为 1';
      return;
    }
    const now = Date.now();
    this.onSave({
      id: 0,
      name: this.name.trim(),
      barcode: this.barcode,
      category: this.category.trim().length === 0 ? '未分类' : this.category.trim(),
      costPriceCents: costPrice,
      salePriceCents: salePrice,
      stockQuantity: 0,
      lowStockThreshold: 5,
      note: '',
      active: true,
      createdAt: now,
      updatedAt: now
    }, quantity);
  }

  build() {
    Column({ space: 12 }) {
      Row() {
        Column({ space: 3 }) {
          Text('录入新商品')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor(PUJI_COLORS.ink)
          Text(`条码 ${this.barcode}`)
            .fontSize(12)
            .fontColor(PUJI_COLORS.inkMuted)
        }
        .alignItems(HorizontalAlign.Start)
        Blank()
        Button('取消')
          .height(PUJI_LAYOUT.minimumTarget)
          .fontColor(PUJI_COLORS.inkMuted)
          .backgroundColor(Color.Transparent)
          .onClick(this.onCancel)
      }

      TextInput({ placeholder: '商品名称（必填）', text: this.name })
        .height(50)
        .backgroundColor(PUJI_COLORS.background)
        .borderRadius(14)
        .onChange((value: string) => {
          this.name = value;
        })
      TextInput({ placeholder: '商品类别，如饮料', text: this.category })
        .height(50)
        .backgroundColor(PUJI_COLORS.background)
        .borderRadius(14)
        .onChange((value: string) => {
          this.category = value;
        })
      Row({ space: 8 }) {
        TextInput({ placeholder: '进货价', text: this.costText })
          .layoutWeight(1)
          .height(50)
          .type(InputType.NUMBER_DECIMAL)
          .backgroundColor(PUJI_COLORS.background)
          .borderRadius(14)
          .onChange((value: string) => {
            this.costText = value;
          })
        TextInput({ placeholder: '销售价', text: this.saleText })
          .layoutWeight(1)
          .height(50)
          .type(InputType.NUMBER_DECIMAL)
          .backgroundColor(PUJI_COLORS.background)
          .borderRadius(14)
          .onChange((value: string) => {
            this.saleText = value;
          })
      }
      TextInput({ placeholder: '本次入货数量', text: this.quantityText })
        .height(50)
        .type(InputType.Number)
        .backgroundColor(PUJI_COLORS.background)
        .borderRadius(14)
        .onChange((value: string) => {
          this.quantityText = value;
        })
      if (this.error.length > 0) {
        Text(this.error)
          .fontSize(13)
          .fontColor(PUJI_COLORS.danger)
          .width('100%')
      }
      Button(this.busy ? '正在保存…' : '确认入货')
        .height(54)
        .width('100%')
        .fontWeight(FontWeight.Bold)
        .fontColor(Color.White)
        .backgroundColor(PUJI_COLORS.primary)
        .borderRadius(17)
        .enabled(!this.busy)
        .onClick(() => {
          this.submit();
        })
    }
    .alignItems(HorizontalAlign.Start)
    .padding(18)
    .width('100%')
    .backgroundColor(PUJI_COLORS.surface)
    .borderRadius(24)
    .border({ width: 2, color: PUJI_COLORS.scanAccent })
  }
}

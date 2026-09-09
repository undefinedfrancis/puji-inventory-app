import { common } from '@kit.AbilityKit';
import { EmptyState, PageHeader, SectionTitle } from '../components/CommonComponents';
import { formatMoney } from '../domain/Money';
import { CsvShareService } from '../export/CsvShareService';
import type { CategoryCount, Product } from '../model/Models';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct InventoryPage {
  @ObjectLink store: PujiStore;
  @State query: string = '';
  @State selectedCategory: string = '全部';
  @State exporting: boolean = false;

  private readonly csvShareService: CsvShareService = new CsvShareService();

  private selectCategory(category: string): void {
    this.selectedCategory = category;
    this.store.reloadProducts(this.query, category);
  }

  private async exportInventory(): Promise<void> {
    if (this.exporting) {
      return;
    }
    this.exporting = true;
    try {
      const content = await this.store.inventoryCsv();
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await this.csvShareService.share(context, '铺记库存.csv', content);
      this.store.message = '库存表已生成，请选择保存或分享位置';
    } catch (error) {
      this.store.message = `导出失败：${error}`;
    } finally {
      this.exporting = false;
    }
  }

  build() {
    Column({ space: 14 }) {
      PageHeader({ title: '货架', subtitle: '按名称、条码或类别快速找到商品。' })

      TextInput({ placeholder: '搜索商品名称或条码', text: this.query })
        .height(52)
        .fontSize(15)
        .fontColor(PUJI_COLORS.ink)
        .placeholderColor(PUJI_COLORS.inkMuted)
        .backgroundColor(PUJI_COLORS.surface)
        .borderRadius(17)
        .border({ width: 1, color: PUJI_COLORS.line })
        .onChange((value: string) => {
          this.query = value;
          this.store.reloadProducts(value, this.selectedCategory);
        })

      Scroll() {
        Row({ space: 8 }) {
          Button('全部')
            .type(ButtonType.Normal)
            .height(PUJI_LAYOUT.minimumTarget)
            .fontSize(14)
            .fontColor(this.selectedCategory === '全部' ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.selectedCategory === '全部' ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(16)
            .onClick(() => {
              this.selectCategory('全部');
            })
          ForEach(this.store.categories, (category: CategoryCount) => {
            Button(`${category.category} ${category.count}`)
              .type(ButtonType.Normal)
              .height(PUJI_LAYOUT.minimumTarget)
              .fontSize(14)
              .fontColor(this.selectedCategory === category.category ? Color.White : PUJI_COLORS.ink)
              .backgroundColor(this.selectedCategory === category.category ? PUJI_COLORS.primary : PUJI_COLORS.surface)
              .borderRadius(16)
              .border({ width: 1, color: this.selectedCategory === category.category ? PUJI_COLORS.primary : PUJI_COLORS.line })
              .onClick(() => {
                this.selectCategory(category.category);
              })
          }, (category: CategoryCount) => category.category)
        }
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')

      SectionTitle({ title: this.selectedCategory === '全部' ? '全部商品' : this.selectedCategory })
      if (this.store.products.length === 0) {
        EmptyState({ title: '还没有商品', message: '前往扫码页输入条码，即可录入第一件商品。' })
      } else {
        List({ space: 8 }) {
          ForEach(this.store.products, (product: Product) => {
            ListItem() {
              Row({ space: 12 }) {
                Column({ space: 5 }) {
                  Row({ space: 7 }) {
                    Text(product.name)
                      .fontSize(16)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(PUJI_COLORS.ink)
                      .maxLines(1)
                    if (product.stockQuantity <= product.lowStockThreshold) {
                      Text('低库存')
                        .fontSize(10)
                        .fontColor(PUJI_COLORS.danger)
                        .padding({ top: 3, right: 7, bottom: 3, left: 7 })
                        .backgroundColor('#F7DDD8')
                        .borderRadius(8)
                    }
                  }
                  Text(`${product.category} · ${product.barcode.length === 0 ? '无条码' : product.barcode}`)
                    .fontSize(12)
                    .fontColor(PUJI_COLORS.inkMuted)
                  Text(`进 ${formatMoney(product.costPriceCents)}  售 ${formatMoney(product.salePriceCents)}`)
                    .fontSize(12)
                    .fontColor(PUJI_COLORS.inkMuted)
                }
                .layoutWeight(1)
                .alignItems(HorizontalAlign.Start)
                Column({ space: 3 }) {
                  Text(product.stockQuantity.toString())
                    .fontSize(24)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(product.stockQuantity <= product.lowStockThreshold ? PUJI_COLORS.danger : PUJI_COLORS.primary)
                  Text('库存')
                    .fontSize(11)
                    .fontColor(PUJI_COLORS.inkMuted)
                }
              }
              .padding(14)
              .width('100%')
              .backgroundColor(PUJI_COLORS.surface)
              .borderRadius(18)
              .border({ width: 1, color: PUJI_COLORS.line })
            }
          }, (product: Product) => product.id.toString())
        }
        .layoutWeight(1)
        .width('100%')
        .scrollBar(BarState.Off)
      }
      Blank()
      Button(this.exporting ? '正在生成…' : '导出库存 CSV')
        .type(ButtonType.Normal)
        .height(52)
        .fontColor(PUJI_COLORS.primary)
        .backgroundColor(PUJI_COLORS.successSoft)
        .borderRadius(17)
        .enabled(!this.exporting)
        .onClick(() => this.exportInventory())
    }
    .padding({ top: 22, right: PUJI_LAYOUT.pagePadding, bottom: 24, left: PUJI_LAYOUT.pagePadding })
    .backgroundColor(PUJI_COLORS.background)
    .width('100%')
    .height('100%')
  }
}

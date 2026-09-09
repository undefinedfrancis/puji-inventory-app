import { EmptyState, MetricCard, PageHeader, SectionTitle } from '../components/CommonComponents';
import { formatMoney } from '../domain/Money';
import { DocumentType } from '../model/Models';
import type { Product } from '../model/Models';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct HomePage {
  @ObjectLink store: PujiStore;
  onInbound: () => void = () => {};
  onOutbound: () => void = () => {};

  private async requestReset(): Promise<void> {
    const result = await this.getUIContext().getPromptAction().showDialog({
      title: '确认清除全部记录？',
      message: '商品、库存、入货、出货和流水都会永久清除，且无法恢复。',
      buttons: [
        { text: '取消', color: PUJI_COLORS.inkMuted },
        { text: '全部清除', color: PUJI_COLORS.danger }
      ]
    });
    if (result.index === 1) {
      await this.store.clearAll();
    }
  }

  build() {
    Scroll() {
      Column({ space: PUJI_LAYOUT.sectionGap }) {
        PageHeader({
          title: '铺记',
          subtitle: '把每一件货、每一笔账，记得清清楚楚。'
        })

        Row({ space: 10 }) {
          Button('＋  入货登记')
            .layoutWeight(1)
            .height(58)
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
            .backgroundColor(PUJI_COLORS.primary)
            .borderRadius(18)
            .onClick(this.onInbound)
          Button('－  出货登记')
            .layoutWeight(1)
            .height(58)
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(PUJI_COLORS.primaryDark)
            .backgroundColor(PUJI_COLORS.warningSoft)
            .borderRadius(18)
            .onClick(this.onOutbound)
        }

        SectionTitle({ title: '今日概况' })
        Grid() {
          GridItem() {
            MetricCard({
              label: '今日销售',
              value: formatMoney(this.store.dashboard.todaySalesCents),
              detail: `${this.store.dashboard.todayItemCount} 件商品`,
              emphasis: true
            })
          }
          GridItem() {
            MetricCard({ label: '库存货值', value: formatMoney(this.store.dashboard.inventoryValueCents), detail: '按进货价估算' })
          }
          GridItem() {
            MetricCard({ label: '商品总数', value: this.store.dashboard.productCount.toString(), detail: '在售商品' })
          }
          GridItem() {
            MetricCard({ label: '低库存', value: this.store.dashboard.lowStockCount.toString(), detail: '需要补货' })
          }
        }
        .columnsTemplate('1fr 1fr')
        .columnsGap(10)
        .rowsGap(10)

        SectionTitle({ title: '补货提醒' })
        if (this.store.products.filter((product: Product) => product.stockQuantity <= product.lowStockThreshold).length === 0) {
          EmptyState({ title: '库存状态良好', message: '低于预警数量的商品会显示在这里。' })
        } else {
          Column({ space: 8 }) {
            ForEach(
              this.store.products.filter((product: Product) => product.stockQuantity <= product.lowStockThreshold),
              (product: Product) => {
                Row() {
                  Column({ space: 4 }) {
                    Text(product.name)
                      .fontSize(15)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(PUJI_COLORS.ink)
                    Text(`${product.category} · 预警 ${product.lowStockThreshold}`)
                      .fontSize(12)
                      .fontColor(PUJI_COLORS.inkMuted)
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)
                  Text(`剩 ${product.stockQuantity}`)
                    .fontSize(15)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(PUJI_COLORS.danger)
                }
                .padding(14)
                .width('100%')
                .backgroundColor(PUJI_COLORS.surface)
                .borderRadius(16)
              },
              (product: Product) => product.id.toString()
            )
          }
        }

        Column({ space: 9 }) {
          Text('数据管理')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(PUJI_COLORS.ink)
            .width('100%')
          Text('按住按钮 2 秒后，还需要在弹窗中再次确认，避免误删。')
            .fontSize(13)
            .fontColor(PUJI_COLORS.inkMuted)
            .lineHeight(19)
            .width('100%')
          Button('按住 2 秒清除全部记录')
            .height(52)
            .width('100%')
            .fontColor(PUJI_COLORS.danger)
            .backgroundColor('#F7DDD8')
            .borderRadius(16)
            .gesture(
              LongPressGesture({ repeat: false, duration: 2000 })
                .onAction(() => {
                  this.requestReset();
                })
            )
        }
        .padding(17)
        .width('100%')
        .backgroundColor(PUJI_COLORS.surfaceMuted)
        .borderRadius(PUJI_LAYOUT.cardRadius)
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

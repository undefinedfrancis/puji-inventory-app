import { EmptyState, MetricCard, PageHeader, SectionTitle } from '../components/CommonComponents';
import { formatMoney } from '../domain/Money';
import { PeriodKind } from '../model/Models';
import type { TrendPoint } from '../model/Models';
import { LedgerPage } from './LedgerPage';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct RecordsPage {
  @ObjectLink store: PujiStore;
  @State showLedger: boolean = false;

  private periodLabel(): string {
    const date = new Date(this.store.periodAnchorMs);
    if (this.store.periodKind === PeriodKind.QUARTER) {
      return `${date.getFullYear()} 年第 ${Math.floor(date.getMonth() / 3) + 1} 季度`;
    }
    if (this.store.periodKind === PeriodKind.YEAR) {
      return `${date.getFullYear()} 年`;
    }
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`;
  }

  build() {
    if (this.showLedger) {
      LedgerPage({
        store: this.store,
        onBack: () => {
          this.showLedger = false;
          this.store.reloadSummary();
        }
      })
    } else {
      Scroll() {
      Column({ space: 14 }) {
        PageHeader({ title: '经营记录', subtitle: '收入、支出和盈利情况，一眼看清。' })

        Row({ space: 7 }) {
          Button('月度')
            .layoutWeight(1)
            .height(48)
            .fontColor(this.store.periodKind === PeriodKind.MONTH ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.store.periodKind === PeriodKind.MONTH ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(15)
            .onClick(() => this.store.setPeriod(PeriodKind.MONTH))
          Button('季度')
            .layoutWeight(1)
            .height(48)
            .fontColor(this.store.periodKind === PeriodKind.QUARTER ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.store.periodKind === PeriodKind.QUARTER ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(15)
            .onClick(() => this.store.setPeriod(PeriodKind.QUARTER))
          Button('年度')
            .layoutWeight(1)
            .height(48)
            .fontColor(this.store.periodKind === PeriodKind.YEAR ? Color.White : PUJI_COLORS.ink)
            .backgroundColor(this.store.periodKind === PeriodKind.YEAR ? PUJI_COLORS.primary : PUJI_COLORS.surface)
            .borderRadius(15)
            .onClick(() => this.store.setPeriod(PeriodKind.YEAR))
        }

        Row() {
          Button('‹')
            .width(48)
            .height(48)
            .fontColor(PUJI_COLORS.primary)
            .backgroundColor(PUJI_COLORS.surface)
            .onClick(() => this.store.movePeriod(-1))
          Blank()
          Text(this.periodLabel())
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor(PUJI_COLORS.ink)
          Blank()
          Button('›')
            .width(48)
            .height(48)
            .fontColor(PUJI_COLORS.inkMuted)
            .backgroundColor(PUJI_COLORS.surfaceMuted)
            .enabled(false)
        }

        Grid() {
          GridItem() {
            MetricCard({ label: '销售收入', value: formatMoney(this.store.summary.salesRevenueCents), emphasis: true })
          }
          GridItem() {
            MetricCard({ label: '进货支出', value: formatMoney(this.store.summary.purchaseExpenseCents) })
          }
          GridItem() {
            MetricCard({
              label: '毛利润',
              value: formatMoney(this.store.summary.grossProfitCents),
              detail: `毛利率 ${(this.store.summary.grossMarginBasisPoints / 100).toFixed(2)}%`
            })
          }
          GridItem() {
            MetricCard({
              label: '现金结余',
              value: formatMoney(this.store.summary.cashBalanceCents),
              detail: `${this.store.summary.transactionCount} 笔交易`
            })
          }
        }
        .columnsTemplate('1fr 1fr')
        .columnsGap(10)
        .rowsGap(10)

        SectionTitle({ title: '经营趋势' })
        if (this.store.summary.trend.length === 0) {
          EmptyState({ title: '暂无趋势', message: '完成入货或出货后，这里会展示经营变化。' })
        } else {
          Row({ space: 5 }) {
            ForEach(this.store.summary.trend, (point: TrendPoint) => {
              Column({ space: 4 }) {
                Stack({ alignContent: Alignment.Bottom }) {
                  Column()
                    .width(9)
                    .height(Math.max(4, Math.min(80, point.revenueCents / 100)))
                    .backgroundColor(PUJI_COLORS.primary)
                    .borderRadius(5)
                }
                .height(82)
                Text(point.label)
                  .fontSize(9)
                  .fontColor(PUJI_COLORS.inkMuted)
              }
              .layoutWeight(1)
            }, (point: TrendPoint) => point.label)
          }
          .padding(14)
          .width('100%')
          .backgroundColor(PUJI_COLORS.surface)
          .borderRadius(PUJI_LAYOUT.cardRadius)
        }

        Button('查阅流水')
          .height(54)
          .width('100%')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(PUJI_COLORS.successSoft)
          .borderRadius(17)
          .onClick(() => {
            this.showLedger = true;
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
}

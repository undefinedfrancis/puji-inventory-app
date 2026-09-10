import { PUJI_COLORS, PUJI_LAYOUT } from '../theme/PujiTheme';

@Component
export struct PageHeader {
  @Prop title: string = '';
  @Prop subtitle: string = '';

  build() {
    Column({ space: 6 }) {
      Text(this.title)
        .fontSize(30)
        .fontWeight(FontWeight.Bold)
        .fontColor(PUJI_COLORS.ink)
        .width('100%')
      if (this.subtitle.length > 0) {
        Text(this.subtitle)
          .fontSize(14)
          .fontColor(PUJI_COLORS.inkMuted)
          .lineHeight(20)
          .width('100%')
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }
}

@Component
export struct SectionTitle {
  @Prop title: string = '';
  @Prop action: string = '';
  onAction: () => void = () => {};

  build() {
    Row() {
      Text(this.title)
        .fontSize(19)
        .fontWeight(FontWeight.Bold)
        .fontColor(PUJI_COLORS.ink)
      Blank()
      if (this.action.length > 0) {
        Button(this.action)
          .type(ButtonType.Normal)
          .height(PUJI_LAYOUT.minimumTarget)
          .fontSize(14)
          .fontColor(PUJI_COLORS.primary)
          .backgroundColor(Color.Transparent)
          .onClick(this.onAction)
      }
    }
    .width('100%')
  }
}

@Component
export struct MetricCard {
  @Prop label: string = '';
  @Prop value: string = '';
  @Prop detail: string = '';
  @Prop emphasis: boolean = false;

  build() {
    Column({ space: 8 }) {
      Text(this.label)
        .fontSize(13)
        .fontColor(this.emphasis ? '#DDECE3' : PUJI_COLORS.inkMuted)
        .width('100%')
      Text(this.value)
        .fontSize(23)
        .fontWeight(FontWeight.Bold)
        .fontColor(this.emphasis ? Color.White : PUJI_COLORS.ink)
        .width('100%')
      if (this.detail.length > 0) {
        Text(this.detail)
          .fontSize(12)
          .fontColor(this.emphasis ? '#DDECE3' : PUJI_COLORS.inkMuted)
          .width('100%')
      }
    }
    .alignItems(HorizontalAlign.Start)
    .padding(16)
    .height(120)
    .width('100%')
    .backgroundColor(this.emphasis ? PUJI_COLORS.primary : PUJI_COLORS.surface)
    .borderRadius(PUJI_LAYOUT.cardRadius)
    .border({ width: this.emphasis ? 0 : 1, color: PUJI_COLORS.line })
  }
}

@Component
export struct EmptyState {
  @Prop title: string = '';
  @Prop message: string = '';

  build() {
    Column({ space: 9 }) {
      Text('○')
        .fontSize(34)
        .fontColor(PUJI_COLORS.primary)
      Text(this.title)
        .fontSize(17)
        .fontWeight(FontWeight.Medium)
        .fontColor(PUJI_COLORS.ink)
      Text(this.message)
        .fontSize(13)
        .fontColor(PUJI_COLORS.inkMuted)
        .textAlign(TextAlign.Center)
        .lineHeight(19)
    }
    .width('100%')
    .padding({ top: 28, right: 22, bottom: 28, left: 22 })
    .backgroundColor(PUJI_COLORS.surface)
    .borderRadius(PUJI_LAYOUT.cardRadius)
    .border({ width: 1, color: PUJI_COLORS.line })
  }
}

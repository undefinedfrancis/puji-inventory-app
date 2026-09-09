import { common } from '@kit.AbilityKit';
import { HomePage } from './HomePage';
import { InventoryPage } from './InventoryPage';
import { RecordsPage } from './RecordsPage';
import { ScannerPage } from './ScannerPage';
import { DocumentType } from '../model/Models';
import { PujiStore } from '../state/PujiStore';
import { PUJI_COLORS, PUJI_TABS } from '../theme/PujiTheme';

@Entry
@Component
struct Index {
  @State store: PujiStore = new PujiStore();

  aboutToAppear(): void {
    const context = this.getUIContext().getHostContext() as common.Context;
    this.store.initialize(context).catch((error: Error) => {
      this.store.message = error.message;
    });
  }

  @Builder
  tabItem(index: number) {
    Column({ space: 3 }) {
      Text(PUJI_TABS[index].symbol)
        .fontSize(index === 2 ? 23 : 20)
        .fontWeight(FontWeight.Bold)
        .fontColor(this.store.selectedTab === index
          ? (PUJI_TABS[index].accent ? PUJI_COLORS.scanAccent : PUJI_COLORS.primary)
          : PUJI_COLORS.inkMuted)
      Text(PUJI_TABS[index].label)
        .fontSize(11)
        .fontWeight(this.store.selectedTab === index ? FontWeight.Bold : FontWeight.Regular)
        .fontColor(this.store.selectedTab === index
          ? (PUJI_TABS[index].accent ? PUJI_COLORS.scanAccent : PUJI_COLORS.primary)
          : PUJI_COLORS.inkMuted)
    }
    .justifyContent(FlexAlign.Center)
    .width('100%')
    .height(58)
  }

  build() {
    Tabs({ barPosition: BarPosition.End, index: this.store.selectedTab }) {
      TabContent() {
        HomePage({
          store: this.store,
          onInbound: () => {
            this.store.setDocumentType(DocumentType.INBOUND);
            this.store.setTab(2);
          },
          onOutbound: () => {
            this.store.setDocumentType(DocumentType.OUTBOUND);
            this.store.setTab(2);
          }
        })
      }
      .tabBar(this.tabItem(0))

      TabContent() {
        InventoryPage({ store: this.store })
      }
      .tabBar(this.tabItem(1))

      TabContent() {
        ScannerPage({ store: this.store })
      }
      .tabBar(this.tabItem(2))

      TabContent() {
        RecordsPage({ store: this.store })
      }
      .tabBar(this.tabItem(3))
    }
    .barHeight(66)
    .barBackgroundColor(PUJI_COLORS.surface)
    .backgroundColor(PUJI_COLORS.background)
    .onChange((index: number) => {
      this.store.setTab(index);
    })
    .width('100%')
    .height('100%')
  }
}

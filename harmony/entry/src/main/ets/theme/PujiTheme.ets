export interface PujiColors {
  background: string;
  surface: string;
  surfaceMuted: string;
  primary: string;
  primaryDark: string;
  scanAccent: string;
  ink: string;
  inkMuted: string;
  line: string;
  danger: string;
  successSoft: string;
  warningSoft: string;
}

export interface PujiLayout {
  pagePadding: number;
  cardRadius: number;
  compactRadius: number;
  sectionGap: number;
  minimumTarget: number;
}

export interface PujiTabDefinition {
  label: string;
  symbol: string;
  accent: boolean;
}

export const PUJI_COLORS: PujiColors = {
  background: '#F5F3EC',
  surface: '#FFFEF9',
  surfaceMuted: '#ECEADF',
  primary: '#276749',
  primaryDark: '#174936',
  scanAccent: '#E8793E',
  ink: '#202722',
  inkMuted: '#657068',
  line: '#D8DDD6',
  danger: '#B54236',
  successSoft: '#DDECE3',
  warningSoft: '#F7E4D2'
};

export const PUJI_LAYOUT: PujiLayout = {
  pagePadding: 18,
  cardRadius: 22,
  compactRadius: 14,
  sectionGap: 14,
  minimumTarget: 48
};

export const PUJI_TABS: PujiTabDefinition[] = [
  { label: '首页', symbol: '⌂', accent: false },
  { label: '货架', symbol: '▦', accent: false },
  { label: '扫码', symbol: '▣', accent: true },
  { label: '记录', symbol: '⌁', accent: false }
];

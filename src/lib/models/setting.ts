export interface SettingRecord {
  key: string;
  value: unknown;
  updatedAt: string;
}

export interface BusinessProfile {
  businessName: string;
  profileTemplate: string;
  defaultDepartment: string | null;
  defaultSupplier: string | null;
  currency: string;
}

export interface DefaultValuesConfig {
  status: string;
  condition: string;
}

export interface ThemeConfig {
  mode: "dark" | "light" | "system";
}

export interface VoiceConfig {
  enabled: boolean;
  provider: string | null;
  language: string;
  wakeWord: string | null;
}

export interface BarcodeConfig {
  defaultSymbology: "code128" | "qr";
  printer: string | null;
}

export interface DatabaseConfig {
  autoBackup: boolean;
  backupIntervalHours: number;
}

export interface AppSettings {
  business: BusinessProfile;
  defaults: DefaultValuesConfig;
  theme: ThemeConfig;
  voice: VoiceConfig;
  barcode: BarcodeConfig;
  database: DatabaseConfig;
}

export const DEFAULT_SETTINGS: AppSettings = {
  business: {
    businessName: "Go VCR",
    profileTemplate: "go-vcr",
    defaultDepartment: null,
    defaultSupplier: null,
    currency: "USD",
  },
  defaults: {
    status: "intake",
    condition: "unknown",
  },
  theme: { mode: "dark" },
  voice: {
    enabled: false,
    provider: null,
    language: "en-US",
    wakeWord: null,
  },
  barcode: {
    defaultSymbology: "code128",
    printer: null,
  },
  database: {
    autoBackup: false,
    backupIntervalHours: 24,
  },
};

export const SETTINGS_KEY = "app-settings";

import { DEFAULT_SETTINGS, SETTINGS_KEY, type AppSettings } from "@/lib/models/setting";
import { settingRepository } from "@/lib/repositories/setting.repository";

export class SettingsService {
  private cache: AppSettings | null = null;

  async load(): Promise<AppSettings> {
    if (this.cache) return this.cache;
    const stored = await settingRepository.getValue<AppSettings>(SETTINGS_KEY);
    this.cache = stored ? this.merge(DEFAULT_SETTINGS, stored) : DEFAULT_SETTINGS;
    return this.cache;
  }

  async save(next: AppSettings): Promise<AppSettings> {
    this.cache = next;
    await settingRepository.setValue(SETTINGS_KEY, next);
    return next;
  }

  async patch(patch: Partial<AppSettings>): Promise<AppSettings> {
    const current = await this.load();
    return this.save(this.merge(current, patch as AppSettings));
  }

  private merge(base: AppSettings, override: Partial<AppSettings>): AppSettings {
    return {
      business: { ...base.business, ...(override.business ?? {}) },
      defaults: { ...base.defaults, ...(override.defaults ?? {}) },
      theme: { ...base.theme, ...(override.theme ?? {}) },
      voice: { ...base.voice, ...(override.voice ?? {}) },
      barcode: { ...base.barcode, ...(override.barcode ?? {}) },
      database: { ...base.database, ...(override.database ?? {}) },
    };
  }
}

export const settingsService = new SettingsService();

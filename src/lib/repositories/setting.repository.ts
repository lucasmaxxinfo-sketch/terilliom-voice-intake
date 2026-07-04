import { StoreName } from "@/lib/db/schema";
import type { SettingRecord } from "@/lib/models/setting";
import { nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class SettingRepository extends BaseRepository<"settings"> {
  constructor() {
    super(StoreName.Settings);
  }

  async getValue<T>(key: string): Promise<T | undefined> {
    const record = await this.getById(key);
    return record?.value as T | undefined;
  }

  async setValue<T>(key: string, value: T): Promise<SettingRecord> {
    const record: SettingRecord = { key, value, updatedAt: nowIso() };
    await this.put(record);
    return record;
  }
}

export const settingRepository = new SettingRepository();

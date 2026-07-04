import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import type { PhotoRecord } from "@/lib/models/photo";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class PhotoRepository extends BaseRepository<"photos"> {
  constructor() {
    super(StoreName.Photos);
  }

  async add(entry: Omit<PhotoRecord, "id" | "createdAt">): Promise<PhotoRecord> {
    const record: PhotoRecord = {
      ...entry,
      id: newId(),
      createdAt: nowIso(),
    };
    await this.put(record);
    return record;
  }

  async listForInventory(inventoryId: string): Promise<PhotoRecord[]> {
    const db = await this.db();
    return db.getAllFromIndex(this.storeName, "by_inventory", inventoryId);
  }
}

export const photoRepository = new PhotoRepository();

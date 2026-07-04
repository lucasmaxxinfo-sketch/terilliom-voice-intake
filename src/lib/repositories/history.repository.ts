import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import type { HistoryRecord } from "@/lib/models/history";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class HistoryRepository extends BaseRepository<"history"> {
  constructor() {
    super(StoreName.History);
  }

  async append(entry: Omit<HistoryRecord, "id" | "createdAt">): Promise<HistoryRecord> {
    const record: HistoryRecord = {
      ...entry,
      id: newId(),
      createdAt: nowIso(),
    };
    await this.put(record);
    return record;
  }

  async listForInventory(inventoryId: string): Promise<HistoryRecord[]> {
    const db = await this.db();
    return db.getAllFromIndex(this.storeName, "by_inventory", inventoryId);
  }

  async listRecent(limit = 50): Promise<HistoryRecord[]> {
    const db = await this.db();
    const tx = db.transaction(this.storeName, "readonly");
    const index = tx.store.index("by_created");
    const results: HistoryRecord[] = [];
    let cursor = await index.openCursor(null, "prev");
    while (cursor && results.length < limit) {
      results.push(cursor.value);
      cursor = await cursor.continue();
    }
    await tx.done;
    return results;
  }
}

export const historyRepository = new HistoryRepository();

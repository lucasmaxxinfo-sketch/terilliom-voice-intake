import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import {
  ImportStatus,
  type ImportRecord,
  type ImportSourceValue,
} from "@/lib/models/import";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class ImportRepository extends BaseRepository<"imports"> {
  constructor() {
    super(StoreName.Imports);
  }

  async create(source: ImportSourceValue, fileName: string | null): Promise<ImportRecord> {
    const record: ImportRecord = {
      id: newId(),
      source,
      fileName,
      status: ImportStatus.Pending,
      totalRows: 0,
      successRows: 0,
      failedRows: 0,
      errors: [],
      createdAt: nowIso(),
      completedAt: null,
    };
    await this.put(record);
    return record;
  }

  async update(id: string, patch: Partial<ImportRecord>): Promise<ImportRecord | undefined> {
    const existing = await this.getById(id);
    if (!existing) return undefined;
    const merged: ImportRecord = { ...existing, ...patch, id: existing.id };
    await this.put(merged);
    return merged;
  }

  async listRecent(limit = 25): Promise<ImportRecord[]> {
    const db = await this.db();
    const tx = db.transaction(this.storeName, "readonly");
    const index = tx.store.index("by_created");
    const results: ImportRecord[] = [];
    let cursor = await index.openCursor(null, "prev");
    while (cursor && results.length < limit) {
      results.push(cursor.value);
      cursor = await cursor.continue();
    }
    await tx.done;
    return results;
  }
}

export const importRepository = new ImportRepository();

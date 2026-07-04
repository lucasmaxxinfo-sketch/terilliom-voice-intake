import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import {
  InventoryCondition,
  InventoryStatus,
  type InventoryRecord,
  type NewInventoryInput,
} from "@/lib/models/inventory";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class InventoryRepository extends BaseRepository<"inventory"> {
  constructor() {
    super(StoreName.Inventory);
  }

  async create(input: NewInventoryInput): Promise<InventoryRecord> {
    const now = nowIso();
    const record: InventoryRecord = {
      id: newId(),
      barcode: input.barcode ?? null,
      brand: input.brand ?? null,
      model: input.model ?? null,
      serialNumber: input.serialNumber ?? null,
      purchaseCost: input.purchaseCost ?? null,
      purchaseSource: input.purchaseSource ?? null,
      supplier: input.supplier ?? null,
      condition: input.condition ?? InventoryCondition.Unknown,
      status: input.status ?? InventoryStatus.Intake,
      department: input.department ?? null,
      notes: input.notes ?? null,
      conversationId: input.conversationId ?? null,
      photoCount: 0,
      attachmentCount: 0,
      metadata: input.metadata ?? {},
      createdAt: now,
      updatedAt: now,
    };
    await this.put(record);
    return record;
  }

  async update(
    id: string,
    patch: Partial<InventoryRecord>,
  ): Promise<InventoryRecord | undefined> {
    const existing = await this.getById(id);
    if (!existing) return undefined;
    const merged: InventoryRecord = {
      ...existing,
      ...patch,
      id: existing.id,
      createdAt: existing.createdAt,
      updatedAt: nowIso(),
    };
    await this.put(merged);
    return merged;
  }

  async listRecent(limit = 20): Promise<InventoryRecord[]> {
    const db = await this.db();
    const tx = db.transaction(this.storeName, "readonly");
    const index = tx.store.index("by_updated");
    const results: InventoryRecord[] = [];
    let cursor = await index.openCursor(null, "prev");
    while (cursor && results.length < limit) {
      results.push(cursor.value);
      cursor = await cursor.continue();
    }
    await tx.done;
    return results;
  }

  async countCreatedSince(iso: string): Promise<number> {
    const db = await this.db();
    const range = IDBKeyRange.lowerBound(iso, false);
    return db.countFromIndex(this.storeName, "by_created", range);
  }

  async findByBarcode(barcode: string): Promise<InventoryRecord | undefined> {
    const db = await this.db();
    return db.getFromIndex(this.storeName, "by_barcode", barcode);
  }

  async findBySerial(serial: string): Promise<InventoryRecord | undefined> {
    const db = await this.db();
    return db.getFromIndex(this.storeName, "by_serial", serial);
  }
}

export const inventoryRepository = new InventoryRepository();

import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import {
  ConversationStatus,
  type ConversationRecord,
} from "@/lib/models/conversation";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class ConversationRepository extends BaseRepository<"conversations"> {
  constructor() {
    super(StoreName.Conversations);
  }

  async start(inventoryId: string | null): Promise<ConversationRecord> {
    const now = nowIso();
    const record: ConversationRecord = {
      id: newId(),
      inventoryId,
      status: ConversationStatus.Active,
      turns: [],
      extractedFields: {},
      createdAt: now,
      updatedAt: now,
    };
    await this.put(record);
    return record;
  }

  async update(
    id: string,
    patch: Partial<ConversationRecord>,
  ): Promise<ConversationRecord | undefined> {
    const existing = await this.getById(id);
    if (!existing) return undefined;
    const merged: ConversationRecord = {
      ...existing,
      ...patch,
      id: existing.id,
      createdAt: existing.createdAt,
      updatedAt: nowIso(),
    };
    await this.put(merged);
    return merged;
  }

  async listForInventory(inventoryId: string): Promise<ConversationRecord[]> {
    const db = await this.db();
    return db.getAllFromIndex(this.storeName, "by_inventory", inventoryId);
  }
}

export const conversationRepository = new ConversationRepository();

import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import type { AttachmentRecord } from "@/lib/models/attachment";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class AttachmentRepository extends BaseRepository<"attachments"> {
  constructor() {
    super(StoreName.Attachments);
  }

  async add(
    entry: Omit<AttachmentRecord, "id" | "createdAt">,
  ): Promise<AttachmentRecord> {
    const record: AttachmentRecord = {
      ...entry,
      id: newId(),
      createdAt: nowIso(),
    };
    await this.put(record);
    return record;
  }

  async listForInventory(inventoryId: string): Promise<AttachmentRecord[]> {
    const db = await this.db();
    return db.getAllFromIndex(this.storeName, "by_inventory", inventoryId);
  }
}

export const attachmentRepository = new AttachmentRepository();

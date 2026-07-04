import { getDB } from "@/lib/db/client";
import { StoreName } from "@/lib/db/schema";
import type { EventRecord, EventTypeValue } from "@/lib/models/event";
import { newId, nowIso } from "@/lib/utils/ids";

import { BaseRepository } from "./base.repository";

export class EventRepository extends BaseRepository<"events"> {
  constructor() {
    super(StoreName.Events);
  }

  async record(type: EventTypeValue, payload: Record<string, unknown> = {}): Promise<EventRecord> {
    const record: EventRecord = {
      id: newId(),
      type,
      payload,
      createdAt: nowIso(),
    };
    await this.put(record);
    return record;
  }

  async listByType(type: EventTypeValue, limit = 100): Promise<EventRecord[]> {
    const db = await this.db();
    const tx = db.transaction(this.storeName, "readonly");
    const index = tx.store.index("by_type");
    const results: EventRecord[] = [];
    let cursor = await index.openCursor(IDBKeyRange.only(type), "prev");
    while (cursor && results.length < limit) {
      results.push(cursor.value);
      cursor = await cursor.continue();
    }
    await tx.done;
    return results;
  }
}

export const eventRepository = new EventRepository();

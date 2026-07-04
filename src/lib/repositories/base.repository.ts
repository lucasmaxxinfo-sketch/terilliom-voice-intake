/**
 * Base repository. Wraps common IndexedDB operations for a single object
 * store. Domain repositories extend this to add typed queries and to keep
 * business logic (audit trail, cascading counts, denormalization) close to
 * the data.
 */
import type { IDBPDatabase, StoreNames } from "idb";

import { getDB } from "@/lib/db/client";
import type { TerilliomDBSchema } from "@/lib/db/schema";

type Schema = TerilliomDBSchema;
type Store = StoreNames<Schema>;

export abstract class BaseRepository<S extends Store> {
  protected constructor(protected readonly storeName: S) {}

  protected async db(): Promise<IDBPDatabase<Schema>> {
    return getDB();
  }

  async getById(id: string): Promise<Schema[S]["value"] | undefined> {
    const db = await this.db();
    return db.get(this.storeName, id as never);
  }

  async list(): Promise<Schema[S]["value"][]> {
    const db = await this.db();
    return db.getAll(this.storeName);
  }

  async count(): Promise<number> {
    const db = await this.db();
    return db.count(this.storeName);
  }

  async put(value: Schema[S]["value"]): Promise<void> {
    const db = await this.db();
    await db.put(this.storeName, value as never);
  }

  async delete(id: string): Promise<void> {
    const db = await this.db();
    await db.delete(this.storeName, id as never);
  }

  async clear(): Promise<void> {
    const db = await this.db();
    await db.clear(this.storeName);
  }
}

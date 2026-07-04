/**
 * Database client. Opens (or upgrades) the single IndexedDB instance and
 * hands out a cached promise so every repository shares one connection.
 *
 * Migrations run inside `upgrade`. Each version's branch is additive and
 * MUST NOT modify or delete branches for earlier versions.
 */
import { openDB, type IDBPDatabase } from "idb";

import { DB_NAME, DB_VERSION, StoreName, type TerilliomDBSchema } from "./schema";

let dbPromise: Promise<IDBPDatabase<TerilliomDBSchema>> | null = null;

export function getDB(): Promise<IDBPDatabase<TerilliomDBSchema>> {
  if (typeof indexedDB === "undefined") {
    return Promise.reject(
      new Error("IndexedDB is not available in this environment."),
    );
  }
  if (!dbPromise) {
    dbPromise = openDB<TerilliomDBSchema>(DB_NAME, DB_VERSION, {
      upgrade(db, oldVersion, _newVersion, tx) {
        runMigrations(db, oldVersion, tx);
      },
      blocked() {
        console.warn("[db] upgrade blocked by another open connection");
      },
      blocking() {
        console.warn("[db] this connection is blocking a newer version");
      },
      terminated() {
        dbPromise = null;
      },
    });
  }
  return dbPromise;
}

export async function closeDB(): Promise<void> {
  if (!dbPromise) return;
  const db = await dbPromise;
  db.close();
  dbPromise = null;
}

function runMigrations(
  db: IDBPDatabase<TerilliomDBSchema>,
  oldVersion: number,
  _tx: unknown,
): void {
  if (oldVersion < 1) {
    const inventory = db.createObjectStore(StoreName.Inventory, { keyPath: "id" });
    inventory.createIndex("by_barcode", "barcode", { unique: false });
    inventory.createIndex("by_serial", "serialNumber", { unique: false });
    inventory.createIndex("by_status", "status", { unique: false });
    inventory.createIndex("by_department", "department", { unique: false });
    inventory.createIndex("by_updated", "updatedAt", { unique: false });
    inventory.createIndex("by_created", "createdAt", { unique: false });

    const history = db.createObjectStore(StoreName.History, { keyPath: "id" });
    history.createIndex("by_inventory", "inventoryId", { unique: false });
    history.createIndex("by_created", "createdAt", { unique: false });

    const attachments = db.createObjectStore(StoreName.Attachments, { keyPath: "id" });
    attachments.createIndex("by_inventory", "inventoryId", { unique: false });

    const photos = db.createObjectStore(StoreName.Photos, { keyPath: "id" });
    photos.createIndex("by_inventory", "inventoryId", { unique: false });

    db.createObjectStore(StoreName.Settings, { keyPath: "key" });

    const events = db.createObjectStore(StoreName.Events, { keyPath: "id" });
    events.createIndex("by_type", "type", { unique: false });
    events.createIndex("by_created", "createdAt", { unique: false });

    const conversations = db.createObjectStore(StoreName.Conversations, { keyPath: "id" });
    conversations.createIndex("by_inventory", "inventoryId", { unique: false });
    conversations.createIndex("by_created", "createdAt", { unique: false });

    const imports = db.createObjectStore(StoreName.Imports, { keyPath: "id" });
    imports.createIndex("by_created", "createdAt", { unique: false });

    db.createObjectStore(StoreName.Metadata, { keyPath: "key" });
  }
}

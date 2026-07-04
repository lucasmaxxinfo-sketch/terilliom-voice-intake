/**
 * IndexedDB schema for Terilliom Intake.
 *
 * All persistent data lives in a single versioned database. Adding a new
 * store or index requires bumping DB_VERSION and adding a migration branch
 * inside `runMigrations` — never mutate existing branches.
 */
import type { DBSchema } from "idb";

import type { AttachmentRecord } from "@/lib/models/attachment";
import type { ConversationRecord } from "@/lib/models/conversation";
import type { EventRecord } from "@/lib/models/event";
import type { HistoryRecord } from "@/lib/models/history";
import type { ImportRecord } from "@/lib/models/import";
import type { InventoryRecord } from "@/lib/models/inventory";
import type { PhotoRecord } from "@/lib/models/photo";
import type { SettingRecord } from "@/lib/models/setting";

export const DB_NAME = "terilliom-intake";
export const DB_VERSION = 1;

export const StoreName = {
  Inventory: "inventory",
  History: "history",
  Attachments: "attachments",
  Photos: "photos",
  Settings: "settings",
  Events: "events",
  Conversations: "conversations",
  Imports: "imports",
  Metadata: "metadata",
} as const;

export type StoreNameValue = (typeof StoreName)[keyof typeof StoreName];

export interface MetadataRecord {
  key: string;
  value: unknown;
  updatedAt: string;
}

export interface TerilliomDBSchema extends DBSchema {
  inventory: {
    key: string;
    value: InventoryRecord;
    indexes: {
      by_barcode: string;
      by_serial: string;
      by_status: string;
      by_department: string;
      by_updated: string;
      by_created: string;
    };
  };
  history: {
    key: string;
    value: HistoryRecord;
    indexes: {
      by_inventory: string;
      by_created: string;
    };
  };
  attachments: {
    key: string;
    value: AttachmentRecord;
    indexes: { by_inventory: string };
  };
  photos: {
    key: string;
    value: PhotoRecord;
    indexes: { by_inventory: string };
  };
  settings: {
    key: string;
    value: SettingRecord;
  };
  events: {
    key: string;
    value: EventRecord;
    indexes: {
      by_type: string;
      by_created: string;
    };
  };
  conversations: {
    key: string;
    value: ConversationRecord;
    indexes: {
      by_inventory: string;
      by_created: string;
    };
  };
  imports: {
    key: string;
    value: ImportRecord;
    indexes: { by_created: string };
  };
  metadata: {
    key: string;
    value: MetadataRecord;
  };
}

/**
 * Inventory domain model.
 *
 * The record shape is intentionally open: business-specific fields live in
 * `metadata`, keyed by field id, so new fields can be added through the
 * settings/profile system without an IndexedDB migration.
 */

export const InventoryStatus = {
  Draft: "draft",
  Intake: "intake",
  InStock: "in_stock",
  Reserved: "reserved",
  Sold: "sold",
  Returned: "returned",
  Scrapped: "scrapped",
} as const;
export type InventoryStatusValue =
  (typeof InventoryStatus)[keyof typeof InventoryStatus];

export const InventoryCondition = {
  New: "new",
  LikeNew: "like_new",
  Good: "good",
  Fair: "fair",
  ForParts: "for_parts",
  Unknown: "unknown",
} as const;
export type InventoryConditionValue =
  (typeof InventoryCondition)[keyof typeof InventoryCondition];

export interface InventoryMetadata {
  [fieldId: string]: string | number | boolean | null | undefined;
}

export interface InventoryRecord {
  id: string;
  barcode: string | null;
  brand: string | null;
  model: string | null;
  serialNumber: string | null;
  purchaseCost: number | null;
  purchaseSource: string | null;
  supplier: string | null;
  condition: InventoryConditionValue;
  status: InventoryStatusValue;
  department: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  conversationId: string | null;
  photoCount: number;
  attachmentCount: number;
  metadata: InventoryMetadata;
}

export type NewInventoryInput = Partial<
  Omit<InventoryRecord, "id" | "createdAt" | "updatedAt" | "photoCount" | "attachmentCount">
>;

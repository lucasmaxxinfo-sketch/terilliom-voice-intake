export const HistoryAction = {
  Created: "created",
  Updated: "updated",
  StatusChanged: "status_changed",
  Deleted: "deleted",
  Imported: "imported",
  Exported: "exported",
  PhotoAdded: "photo_added",
  AttachmentAdded: "attachment_added",
} as const;
export type HistoryActionValue = (typeof HistoryAction)[keyof typeof HistoryAction];

export interface HistoryRecord {
  id: string;
  inventoryId: string;
  action: HistoryActionValue;
  field: string | null;
  previousValue: string | null;
  nextValue: string | null;
  actor: string | null;
  createdAt: string;
}

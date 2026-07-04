export interface PhotoRecord {
  id: string;
  inventoryId: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  width: number | null;
  height: number | null;
  blob: Blob;
  createdAt: string;
}

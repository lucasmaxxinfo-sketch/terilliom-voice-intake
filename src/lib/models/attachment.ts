export interface AttachmentRecord {
  id: string;
  inventoryId: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  blob: Blob;
  createdAt: string;
}

export const ImportStatus = {
  Pending: "pending",
  Processing: "processing",
  Completed: "completed",
  Failed: "failed",
} as const;
export type ImportStatusValue = (typeof ImportStatus)[keyof typeof ImportStatus];

export const ImportSource = {
  Csv: "csv",
  Json: "json",
  Manual: "manual",
} as const;
export type ImportSourceValue = (typeof ImportSource)[keyof typeof ImportSource];

export interface ImportRecord {
  id: string;
  source: ImportSourceValue;
  fileName: string | null;
  status: ImportStatusValue;
  totalRows: number;
  successRows: number;
  failedRows: number;
  errors: string[];
  createdAt: string;
  completedAt: string | null;
}

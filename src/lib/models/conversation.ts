export type ConversationRole = "user" | "assistant" | "system";

export interface ConversationTurn {
  id: string;
  role: ConversationRole;
  content: string;
  createdAt: string;
  confidence: number | null;
}

export const ConversationStatus = {
  Active: "active",
  Completed: "completed",
  Abandoned: "abandoned",
} as const;
export type ConversationStatusValue =
  (typeof ConversationStatus)[keyof typeof ConversationStatus];

export interface ConversationRecord {
  id: string;
  inventoryId: string | null;
  status: ConversationStatusValue;
  turns: ConversationTurn[];
  extractedFields: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

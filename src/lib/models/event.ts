export const EventType = {
  AppStart: "app_start",
  IntakeStarted: "intake_started",
  IntakeCompleted: "intake_completed",
  VoiceUtterance: "voice_utterance",
  AiCall: "ai_call",
  BarcodeScanned: "barcode_scanned",
  Error: "error",
} as const;
export type EventTypeValue = (typeof EventType)[keyof typeof EventType];

export interface EventRecord {
  id: string;
  type: EventTypeValue;
  payload: Record<string, unknown>;
  createdAt: string;
}

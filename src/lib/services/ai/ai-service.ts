/**
 * AI service abstraction. A concrete implementation is registered at runtime
 * (see `registerAiService`). No provider is hardcoded — the intake flow only
 * ever depends on this interface.
 */

export interface AiMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export interface AiIntent {
  name: string;
  confidence: number;
  parameters: Record<string, unknown>;
}

export interface AiExtraction<TFields = Record<string, unknown>> {
  fields: TFields;
  confidence: number;
  missing: string[];
}

export interface AiResponse {
  content: string;
  confidence: number;
  raw?: unknown;
}

export interface AiConversationRequest {
  messages: AiMessage[];
  schema?: unknown;
  signal?: AbortSignal;
}

export interface AiFieldExtractionRequest {
  transcript: string;
  fields: string[];
  hints?: Record<string, unknown>;
  signal?: AbortSignal;
}

export interface AiIntentRequest {
  transcript: string;
  intents: string[];
  signal?: AbortSignal;
}

export interface AiService {
  readonly id: string;
  readonly available: boolean;

  respond(request: AiConversationRequest): Promise<AiResponse>;
  extractFields<TFields = Record<string, unknown>>(
    request: AiFieldExtractionRequest,
  ): Promise<AiExtraction<TFields>>;
  detectIntent(request: AiIntentRequest): Promise<AiIntent>;
}

class UnavailableAiService implements AiService {
  readonly id = "unavailable";
  readonly available = false;

  private fail(): never {
    throw new Error(
      "No AI service is registered. Call registerAiService() with a provider before using AI features.",
    );
  }

  respond(): Promise<AiResponse> {
    this.fail();
  }
  extractFields<TFields>(): Promise<AiExtraction<TFields>> {
    this.fail();
  }
  detectIntent(): Promise<AiIntent> {
    this.fail();
  }
}

let current: AiService = new UnavailableAiService();

export function registerAiService(service: AiService): void {
  current = service;
}

export function getAiService(): AiService {
  return current;
}

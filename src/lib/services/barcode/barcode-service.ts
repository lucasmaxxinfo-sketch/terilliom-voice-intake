/**
 * Barcode service abstraction. Handles generation, validation and (later)
 * printer dispatch. Concrete generation is not implemented in this stage.
 */

export type BarcodeSymbology = "code128" | "qr";

export interface BarcodeGenerateOptions {
  value: string;
  symbology: BarcodeSymbology;
  width?: number;
  height?: number;
  margin?: number;
  includeText?: boolean;
}

export interface BarcodeResult {
  symbology: BarcodeSymbology;
  value: string;
  /** SVG markup for the generated barcode. */
  svg: string;
}

export interface PrinterInfo {
  id: string;
  name: string;
  connection: "usb" | "bluetooth" | "network" | "unknown";
}

export interface PrintOptions {
  printerId: string;
  copies?: number;
}

export interface BarcodeService {
  readonly id: string;
  readonly available: boolean;

  generate(options: BarcodeGenerateOptions): Promise<BarcodeResult>;
  validate(value: string, symbology: BarcodeSymbology): boolean;
  format(value: string, symbology: BarcodeSymbology): string;

  listPrinters(): Promise<PrinterInfo[]>;
  print(result: BarcodeResult, options: PrintOptions): Promise<void>;
}

function validate(value: string, symbology: BarcodeSymbology): boolean {
  if (!value || value.length === 0 || value.length > 256) return false;
  if (symbology === "code128") {
    // Code128 supports the full ASCII 0-127 range.
    for (let i = 0; i < value.length; i++) {
      const code = value.charCodeAt(i);
      if (code < 0 || code > 127) return false;
    }
    return true;
  }
  // QR accepts any UTF-8 string within a practical size.
  return true;
}

function format(value: string, symbology: BarcodeSymbology): string {
  const trimmed = value.trim();
  if (symbology === "code128") {
    return trimmed.toUpperCase().replace(/\s+/g, "-");
  }
  return trimmed;
}

class UnavailableBarcodeService implements BarcodeService {
  readonly id = "unavailable";
  readonly available = false;

  private fail(op: string): never {
    throw new Error(
      `No barcode service is registered — ${op} is unavailable. Call registerBarcodeService() with a provider.`,
    );
  }

  generate(): Promise<BarcodeResult> {
    this.fail("generate");
  }
  validate = validate;
  format = format;
  listPrinters(): Promise<PrinterInfo[]> {
    return Promise.resolve([]);
  }
  print(): Promise<void> {
    this.fail("print");
  }
}

let current: BarcodeService = new UnavailableBarcodeService();

export function registerBarcodeService(service: BarcodeService): void {
  current = service;
}

export function getBarcodeService(): BarcodeService {
  return current;
}

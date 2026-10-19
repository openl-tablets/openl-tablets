/** Shared helpers for transporting text and binary file content over MCP. */

/** Detect binary data using NUL/control-byte sampling. */
function looksBinary(buffer: Buffer): boolean {
  const sampleLength = Math.min(buffer.length, 8000);
  if (sampleLength === 0) return false;
  let suspicious = 0;
  for (let index = 0; index < sampleLength; index += 1) {
    const byte = buffer[index];
    if (byte === 0) return true;
    if (byte < 9 || (byte > 13 && byte < 32) || byte === 127) suspicious += 1;
  }
  return suspicious / sampleLength > 0.1;
}

function isUtf8ContinuationByte(byte: number | undefined): boolean {
  return byte !== undefined && (byte & 0xc0) === 0x80;
}

/** Move byte boundaries forward so a UTF-8 chunk never starts or ends inside a code point. */
function alignUtf8Chunk(data: Buffer, requestedStart: number, requestedEnd: number): [number, number] {
  let start = requestedStart;
  while (start < data.length && isUtf8ContinuationByte(data[start])) start += 1;

  let end = Math.max(start, requestedEnd);
  while (end < data.length && isUtf8ContinuationByte(data[end])) end += 1;
  return [start, end];
}

/** The part of a file a read returns, and whether it goes as base64 or as text. */
interface ContentSlice {
  start: number;
  end: number;
  slice: Buffer;
  asBase64: boolean;
}

/**
 * The bytes a read returns of `data`: at most `length` from `offset`, clamped to the data.
 *
 * They go as base64 when `encoding` asks for it, or when `encoding` is `auto` or absent and they look binary. A text
 * chunk keeps its characters whole: its boundaries move forward to the next character.
 */
export function sliceContent(
  data: Buffer,
  range: { offset?: number; length?: number; encoding?: "auto" | "utf-8" | "base64" },
): ContentSlice {
  const requestedStart = Math.min(range.offset ?? 0, data.length);
  const requestedEnd = Math.min(requestedStart + (range.length ?? data.length), data.length);
  const asBase64 = range.encoding === "base64"
    || (range.encoding !== "utf-8" && looksBinary(data.subarray(requestedStart, requestedEnd)));
  const [start, end] = asBase64 ? [requestedStart, requestedEnd] : alignUtf8Chunk(data, requestedStart, requestedEnd);
  return { start, end, slice: data.subarray(start, end), asBase64 };
}

/** Remove line wrapping accepted by the legacy base64 file-write contract. */
export function compactBase64(value: string): string {
  return value.replace(/\s+/g, "");
}

/** Validate standard base64 after accepting legacy whitespace wrapping. */
export function isValidBase64(value: string): boolean {
  // Only canonical standard base64 survives decoding and encoding again unchanged.
  const compact = compactBase64(value);
  return Buffer.from(compact, "base64").toString("base64") === compact;
}

/** Normalize an HTTP Content-Type value for an MCP resource block. */
export function normalizeMimeType(contentType: string): string {
  return contentType.split(";", 1)[0]?.trim().toLowerCase() || "application/octet-stream";
}

/** Shared helpers for transporting text and binary file content over MCP. */

/** Detect binary data using NUL/control-byte sampling. */
export function looksBinary(buffer: Buffer): boolean {
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

/** Remove line wrapping accepted by the legacy base64 file-write contract. */
export function compactBase64(value: string): string {
  return value.replace(/\s+/g, "");
}

/** Validate standard base64 after accepting legacy whitespace wrapping. */
export function isValidBase64(value: string): boolean {
  const compact = compactBase64(value);
  if (compact.length === 0) return true;
  if (compact.length % 4 !== 0) return false;
  if (!/^[A-Za-z0-9+/]*={0,2}$/.test(compact)) return false;
  return Buffer.from(compact, "base64").toString("base64") === compact;
}

/** Normalize an HTTP Content-Type value for an MCP resource block. */
export function normalizeMimeType(contentType: string): string {
  return contentType.split(";", 1)[0]?.trim().toLowerCase() || "application/octet-stream";
}

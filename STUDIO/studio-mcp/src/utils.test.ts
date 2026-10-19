/**
 * Unit tests for utils.ts: the redaction of secrets in error messages and in the context of a logged error, and the
 * reading of the error answers of OpenL Studio.
 */

import { describe, it, expect } from "vitest";
import {
  extractApiErrorInfo,
  sanitizeError,
  sanitizeJson,
} from "./utils.js";

describe("utils", () => {
  describe("sanitizeError", () => {
    it.each([
      ["a bearer token", new Error("Authorization failed: Bearer abc123def456"), ["Bearer [REDACTED]"], ["abc123"]],
      ["credentials in a URL", new Error("Failed to connect to http://user:password@example.com"),
        ["[REDACTED]:[REDACTED]@"], ["user:", ":password"]],
      ["a client secret", new Error("OAuth failed: client_secret=very_secret"), ["client_secret=[REDACTED]"], ["very_secret"]],
      ["a quoted client-secret", new Error('client-secret: "secret123"'), ["[REDACTED]"], ["secret123"]],
      ["a camel-case clientSecret", new Error("clientSecret=secret456"), ["[REDACTED]"], ["secret456"]],
      ["a token in a plain string", "Bearer token123", ["Bearer [REDACTED]"], ["token123"]],
      ["a token in the message of an object", { message: "Token abc123" }, ["Token [REDACTED]"], ["abc123"]],
      ["several secrets at once", new Error("OAuth failed: client_secret=abc123 Bearer token456 at http://user:pass@host"),
        ["Bearer [REDACTED]", "[REDACTED]:[REDACTED]@"], ["abc123", "token456", "user:", ":pass"]],
    ])("redacts %s", (_case, error, kept, removed) => {
      const result = sanitizeError(error);

      for (const text of kept) expect(result).toContain(text);
      for (const text of removed) expect(result).not.toContain(text);
    });

    it.each([
      ["plain string", "plain string"],
      ["The token has expired; bearer of bad news", "The token has expired; bearer of bad news"],
      [new Error("Invalid parameter value"), "Invalid parameter value"],
      [{ message: "Error occurred" }, "Error occurred"],
      [null, "Unknown error"],
      [undefined, "Unknown error"],
      [{ code: 500 }, "Unknown error"],
      [{ message: 123 }, "Unknown error"],
    ])("tells %j as %j", (error, message) => {
      expect(sanitizeError(error)).toBe(message);
    });
  });

  describe("sanitizeJson", () => {
    it("redacts a Buffer body to a length marker instead of a per-byte map", () => {
      // Regression: openl_write_project_file sends an octet-stream Buffer; if it
      // reached error context it must NOT serialize byte-for-byte.
      const secret = Buffer.from("api content with SUPER_SECRET_VALUE_12345");
      const result = sanitizeJson(secret) as string;
      expect(result).toBe(`[binary: ${secret.length} bytes]`);
      expect(result).not.toContain("SUPER_SECRET");
      expect(result).not.toMatch(/"0":/);
    });

    it("redacts a Buffer nested inside an object", () => {
      const out = sanitizeJson({ requestData: Buffer.from("secret file body") }) as Record<string, unknown>;
      expect(out.requestData).toBe(`[binary: 16 bytes]`);
    });

    it("redacts an ArrayBuffer / typed array", () => {
      const ab = new ArrayBuffer(8);
      expect(sanitizeJson(ab)).toBe("[binary: 8 bytes]");
      expect(sanitizeJson(new Uint8Array([1, 2, 3]))).toBe("[binary: 3 bytes]");
    });

    it("redacts an oversized string value (e.g. a raw file content arg)", () => {
      const big = "x".repeat(5000);
      const out = sanitizeJson({ content: big }) as Record<string, unknown>;
      expect(out.content).toBe("[redacted: 5000 chars]");
    });

    it("leaves short non-sensitive string values intact (after pattern redaction)", () => {
      const out = sanitizeJson({ path: "rules/Model.xlsx" }) as Record<string, unknown>;
      expect(out.path).toBe("rules/Model.xlsx");
    });

    it("redacts a sensitive substring in a non-sensitive key's value via pattern matching", () => {
      // 'note' is not a sensitive key, so the value is pattern-scanned.
      const out = sanitizeJson({ note: "Bearer abc123secrettoken" }) as Record<string, unknown>;
      expect(out.note).toBe("Bearer [REDACTED]");
      expect(out.note).not.toContain("abc123secrettoken");
    });

    it("applies the 2048-char length cap at the boundary", () => {
      const exactly2048 = "x".repeat(2048);
      const over = "x".repeat(2049);
      // > 2048 is redacted to a length marker; exactly 2048 passes through.
      expect(sanitizeJson(exactly2048)).toBe(exactly2048);
      expect(sanitizeJson(over)).toBe("[redacted: 2049 chars]");
    });
  });

  describe("extractApiErrorInfo", () => {
    it.each([
      ["a body that is no object", "Bad Gateway", 502],
      ["an object without a code or a message", { detail: "x" }, 404],
    ])("keeps %s as the raw response", (_case, body, status) => {
      expect(extractApiErrorInfo(body, status)).toEqual({ rawResponse: body });
    });
  });
});

/**
 * Utility functions for the OpenL MCP Server
 */

import type { ProjectViewModel, ResultNotReadyView } from "./types.js";

/** Error detail in API error response (for 400 status) */
interface ApiErrorDetail {
  code?: string;
  message?: string;
}

/** Field validation error in API error response (for 400 status) */
interface ApiFieldError {
  code?: string;
  field?: string;
  message?: string;
  rejectedValue?: unknown;
}

/** Extracted error information from API response */
export interface ExtractedErrorInfo {
  code?: string;
  message?: string;
  errors?: ApiErrorDetail[];
  fields?: ApiFieldError[];
  rawResponse?: unknown; // Original response data if structure doesn't match expected formats
}

// Shared sanitization patterns
const SENSITIVE_PATTERNS: Array<[RegExp, string]> = [
  // The schemes as an Authorization header writes them, so that prose ("the token has expired") is left alone
  [/\bBearer\s+[\w.~+/-]+=*/g, "Bearer [REDACTED]"],
  [/\bToken\s+[\w.~+/-]+=*/g, "Token [REDACTED]"],
  [/openl_pat_[\w.~+/-]+/gi, "openl_pat_[REDACTED]"],
  [/api[_-]?key["\s:=]+[\w.~+/-]+/gi, "api_key=[REDACTED]"],
  [/(:\/\/)[^:@]+:[^@]+@/g, "$1[REDACTED]:[REDACTED]@"],
  [/client[_-]?secret["\s:=]+[\w.~+/-]+/gi, "client_secret=[REDACTED]"],
  [/authorization[_-]?code["\s:=]+[\w.~+/-]+/gi, "authorization_code=[REDACTED]"],
  [/refresh[_-]?token["\s:=]+[\w.~+/-]+/gi, "refresh_token=[REDACTED]"],
  [/code[_-]?verifier["\s:=]+[\w.~+/-]+/gi, "code_verifier=[REDACTED]"],
];

// Sensitive keys for JSON sanitization, compared lower-case without '_' and '-', so that
// api_key, apiKey, ApiKey and api-key all match "apikey"
const SENSITIVE_KEYS = new Set([
  "password",
  "token",
  "secret",
  "apikey",
  "authorization",
  "auth",
  "credential",
  "pat",
  "personalaccesstoken",
  "accesstoken",
  "refreshtoken",
  "clientsecret",
]);

function isSensitiveKey(key: string): boolean {
  return SENSITIVE_KEYS.has(key.toLowerCase().replace(/[-_]/g, ""));
}

/**
 * Apply sanitization patterns to a string to redact sensitive data
 *
 * @param str - String to sanitize
 * @returns Sanitized string with sensitive patterns redacted
 */
function applySanitizationPatterns(str: string): string {
  return SENSITIVE_PATTERNS.reduce((s, [pattern, replacement]) => s.replace(pattern, replacement), str);
}

/**
 * Sanitize error messages to prevent sensitive data exposure
 *
 * @param error - Error object, string, or object with message property to sanitize
 * @returns Sanitized error message
 */
export function sanitizeError(error: unknown): string {
  if (typeof error === "string") {
    return applySanitizationPatterns(error);
  }
  // An Error, or any other object with a message
  const message = (error as { message?: unknown } | null | undefined)?.message;
  return typeof message === "string" ? applySanitizationPatterns(message) : "Unknown error";
}

/**
 * Upper bound on the length of a string value preserved in sanitized error
 * context. Long string values (e.g. a raw file body passed to
 * openl_write_project_file via its `content` arg) are replaced with a length
 * marker so proprietary rules / PII / pasted secrets can't leak into logs or
 * into the ProtocolError data returned to the client. Generous enough for normal
 * args (paths, comments, ids).
 */
const MAX_SANITIZED_STRING_LENGTH = 2048;

/**
 * Sanitize JSON object to prevent sensitive data exposure in logs
 * Recursively removes or redacts sensitive fields (tokens, passwords, secrets, etc.)
 *
 * @param obj - Object to sanitize
 * @returns Sanitized object (deep clone)
 */
export function sanitizeJson(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }

  // Binary payloads (e.g. an octet-stream Buffer request body) must NEVER be
  // walked as plain objects: Object.entries(buffer) yields a {0:byte,1:byte,…}
  // map that serializes the full content byte-for-byte, bypassing redaction.
  // A Buffer is a view too.
  if (ArrayBuffer.isView(obj) || obj instanceof ArrayBuffer) {
    return `[binary: ${obj.byteLength} bytes]`;
  }

  if (typeof obj === "string") {
    if (obj.length > MAX_SANITIZED_STRING_LENGTH) {
      return `[redacted: ${obj.length} chars]`;
    }
    // Apply string sanitization
    return applySanitizationPatterns(obj);
  }

  if (typeof obj !== "object") {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(item => sanitizeJson(item));
  }

  // Handle objects
  const sanitized: Record<string, unknown> = {};

  for (const [key, value] of Object.entries(obj)) {
    sanitized[key] = isSensitiveKey(key) ? "[REDACTED]" : sanitizeJson(value);
  }

  return sanitized;
}

/** Type guard for the 202 body returned while an asynchronous result is not ready. */
export function isResultNotReady(value: unknown): value is ResultNotReadyView {
  return isPlainObject(value) && value.status === "notReady";
}

/**
 * Whether a project has no working copy to commit: it is neither opened nor closed, so every change lands in its
 * files at once. A project edited in place is one, and so is a project of the 'local' repository. A project whose
 * capabilities are not told is taken for one with a working copy.
 */
export function isEditedInPlace({ capabilities }: ProjectViewModel): boolean {
  return capabilities !== undefined && !capabilities.canOpen && !capabilities.canClose;
}

/**
 * Type guard for a plain (non-null, non-array) object.
 *
 * @param value - Value to check
 * @returns True if value is a plain object usable as a string-keyed record
 */
export function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** The names of the flags that are set: `["canOpen", "canSave"]` of `{ canOpen: true, canSave: true, canMerge: false }`. */
export function namesOfSet(flags: object | undefined): string[] {
  return Object.entries(flags ?? {}).filter(([, set]) => set === true).map(([name]) => name);
}

/**
 * Extract structured error information from API response data
 *
 * Handles different error response formats:
 * - 400: {code, errors[], fields[], message}
 * - 401-500: {code, message}
 * - Unknown formats: returns raw response
 *
 * @param responseData - Response data of a failed request
 * @param status - HTTP status code
 * @returns Extracted error information
 */
export function extractApiErrorInfo(
  responseData: unknown,
  status?: number
): ExtractedErrorInfo {
  if (!isPlainObject(responseData)) {
    return {
      rawResponse: responseData,
    };
  }

  const result: ExtractedErrorInfo = {
    ...(typeof responseData.code === "string" ? { code: responseData.code } : {}),
    ...(typeof responseData.message === "string" ? { message: responseData.message } : {}),
  };

  // Handle 400 Bad Request format
  if (status === 400) {
    // Extract errors array if present
    if (Array.isArray(responseData.errors)) {
      result.errors = responseData.errors.filter(isPlainObject).map((err) => ({
        code: typeof err.code === "string" ? err.code : undefined,
        message: typeof err.message === "string" ? err.message : undefined,
      }));
    }

    // Extract fields array if present
    if (Array.isArray(responseData.fields)) {
      result.fields = responseData.fields.filter(isPlainObject).map((field) => ({
        code: typeof field.code === "string" ? field.code : undefined,
        field: typeof field.field === "string" ? field.field : undefined,
        message: typeof field.message === "string" ? field.message : undefined,
        rejectedValue: field.rejectedValue,
      }));
    }

    // If we extracted at least some structured data, return it
    if (result.code || result.message || result.errors || result.fields) {
      return result;
    }
  } else if (status && status >= 401 && status <= 500 && (result.code || result.message)) {
    // Handle 401-500 format
    return result;
  }

  // Unknown format - return raw response
  return {
    rawResponse: responseData,
  };
}

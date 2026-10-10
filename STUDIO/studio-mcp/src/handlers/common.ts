/**
 * Shared core for the OpenL tool handlers.
 *
 * Holds the tool registry (the `registerTool` / `getAllTools` / `executeTool`
 * trio and the `ToolDefinition` shape) plus the cross-cutting error handling
 * that every tool shares. The per-category handler modules in this directory
 * import from here and never the other way around, so this file has no
 * dependency on any handler module and the registry lives in exactly one place.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { ContentBlock, ProgressToken, ServerNotification } from "@modelcontextprotocol/server";
import { z, type ZodType } from "zod";

import type { OpenLClient } from "../client.js";
import type { ToolCategory } from "../constants.js";
import type { ExtractedErrorInfo } from "../types.js";
import { logger } from "../logger.js";
import { isAxiosError, sanitizeError, extractApiErrorInfo, sanitizeJson } from "../utils.js";

/**
 * Tool response structure
 */
export interface ToolResponse {
  [key: string]: unknown;
  // Every OpenL result starts with a text summary so CLI and older MCP clients
  // remain useful; protocol-native image/audio/resource blocks may follow it.
  content: [{ type: "text"; text: string }, ...ContentBlock[]];
  structuredContent?: Record<string, unknown>;
  isError?: boolean;
}

/**
 * Per-request context the MCP SDK passes to request handlers. Carries the optional
 * `progressToken` (under `_meta`), a `sendNotification` callback bound to the calling
 * session's transport, and an `AbortSignal` that fires when the client cancels.
 */
export interface ToolHandlerExtra {
  signal: AbortSignal;
  _meta?: { progressToken?: ProgressToken };
  sendNotification?: (notification: ServerNotification) => Promise<void>;
}

/**
 * Tool handler function type
 */
type ToolHandler<TSchema extends ZodType> = (
  args: z.output<TSchema>,
  client: OpenLClient,
  extra?: ToolHandlerExtra,
) => Promise<ToolResponse>;

/**
 * Tool definition with MCP metadata
 */
export interface ToolDefinition<TSchema extends ZodType = ZodType> {
  name: string;
  title: string;
  description: string;
  /** Display category for CLI `--help` grouping. */
  category: ToolCategory;
  /**
   * Single source of truth for both the JSON Schema advertised over MCP and the
   * arguments accepted at runtime.
   */
  schema: TSchema;
  annotations?: {
    readOnlyHint?: boolean;
    openWorldHint?: boolean;
    idempotentHint?: boolean;
    destructiveHint?: boolean;
  };
  /**
   * Optional pre-schema coercion or domain-specific validation of raw arguments.
   * The returned value is always validated against `schema` before the handler
   * runs. Used by structured-payload table tools for JSON-string coercion and
   * richer discriminator errors.
   */
  validateArgs?: (args: unknown) => unknown;
  /** Optional tool-specific enrichment for schema validation failures. */
  formatValidationError?: (error: z.ZodError) => string;
  handler: ToolHandler<TSchema>;
}

/**
 * Registry of all tool handlers
 */
const toolHandlers = new Map<string, ToolDefinition>();
// JSON Schema conversion is expensive; registered Zod schemas are immutable and reusable by identity.
const inputSchemaCache = new WeakMap<ZodType, Record<string, unknown>>();

/** Add discovery hints for identifiers rejected before a handler can run. */
function formatSchemaValidationError(name: string, error: z.ZodError): string {
  let message = `Invalid arguments for ${name}:\n${z.prettifyError(error)}`;
  const topLevelFields = new Set(error.issues.map((issue) => issue.path[0]));
  const hints: string[] = [];
  if (topLevelFields.has("projectId")) {
    hints.push("Use openl_list_projects() to find valid project IDs.");
  }
  if (topLevelFields.has("tableId")) {
    hints.push("Use openl_list_tables() to find valid table IDs.");
  }
  if (topLevelFields.has("repository")) {
    hints.push("Use openl_list_repositories() to find valid repositories.");
  }
  if (hints.length > 0) {
    message += `\n\n${hints.join(" ")}`;
  }
  return message;
}

/**
 * Zod's default `z.object()` parser strips unknown nested keys even though its
 * generated JSON Schema advertises `additionalProperties: false`. Detect those
 * stripped keys so runtime acceptance cannot be looser than the MCP contract.
 */
function findStrippedPaths(input: unknown, parsed: unknown): string[] {
  const strippedPaths: string[] = [];
  // Reuse one path stack because raw table payloads may contain thousands of nested cells.
  const path: string[] = [];

  const visit = (inputValue: unknown, parsedValue: unknown): void => {
    if (Array.isArray(inputValue) && Array.isArray(parsedValue)) {
      for (let index = 0; index < inputValue.length && index < parsedValue.length; index += 1) {
        path.push(String(index));
        visit(inputValue[index], parsedValue[index]);
        path.pop();
      }
      return;
    }
    if (
      inputValue === null || parsedValue === null ||
      typeof inputValue !== "object" || typeof parsedValue !== "object" ||
      Array.isArray(inputValue) || Array.isArray(parsedValue)
    ) {
      return;
    }

    const parsedRecord = parsedValue as Record<string, unknown>;
    for (const [key, value] of Object.entries(inputValue as Record<string, unknown>)) {
      path.push(key);
      if (!Object.prototype.hasOwnProperty.call(parsedRecord, key)) {
        strippedPaths.push(path.join("."));
      } else {
        visit(value, parsedRecord[key]);
      }
      path.pop();
    }
  };

  visit(input, parsed);
  return strippedPaths;
}

function getInputSchema(schema: ZodType): Record<string, unknown> {
  let inputSchema = inputSchemaCache.get(schema);
  if (!inputSchema) {
    inputSchema = z.toJSONSchema(schema) as Record<string, unknown>;
    // MCP tool input schemas always have an object root. Zod emits object-only
    // unions as top-level oneOf without repeating that shared root type.
    inputSchema.type ??= "object";
    inputSchemaCache.set(schema, inputSchema);
  }
  return inputSchema;
}

/**
 * Register a single tool with the registry
 *
 * @param tool - Tool definition with handler
 */
export function registerTool<TSchema extends ZodType>(tool: ToolDefinition<TSchema>): void {
  // The schema/handler pair is type-checked together at registration. The map
  // erases that generic relationship, then executeTool restores it by parsing
  // with the stored schema before invoking the stored handler.
  toolHandlers.set(tool.name, tool as ToolDefinition);
}

/**
 * Whether a tool with this bare (un-prefixed) name is registered. Lets the
 * transport layer tell a genuinely unknown tool (a protocol fault) apart from a
 * registered tool that failed at runtime — the two are otherwise indistinguishable
 * once both surface as a `ProtocolError` (e.g. a backend HTTP 405 also maps to
 * `ProtocolErrorCode.MethodNotFound`).
 */
export function hasTool(name: string): boolean {
  return toolHandlers.has(name);
}

/**
 * Get all registered tools (for ListTools handler)
 *
 * @returns Array of tool definitions without the handler or validation callbacks
 */
export function getAllTools(): Array<
  Omit<ToolDefinition, "handler" | "validateArgs" | "formatValidationError" | "schema"> & {
    inputSchema: Record<string, unknown>;
  }
> {
  return Array.from(toolHandlers.values()).map(
    ({ handler: _handler, validateArgs: _validateArgs, formatValidationError: _formatValidationError, schema, ...tool }) => ({
      ...tool,
      inputSchema: getInputSchema(schema),
    }),
  );
}

/**
 * Execute a tool by name
 *
 * @param name - Tool name
 * @param args - Tool arguments
 * @param client - OpenL client instance
 * @returns Tool execution result
 */
export async function executeTool(
  name: string,
  args: unknown,
  client: OpenLClient,
  extra?: ToolHandlerExtra,
): Promise<ToolResponse> {
  const tool = toolHandlers.get(name);
  if (!tool) {
    throw new ProtocolError(ProtocolErrorCode.MethodNotFound, `Unknown tool: ${name}`);
  }

  try {
    const rawArgs = args ?? {};
    const preparedArgs = tool.validateArgs ? tool.validateArgs(rawArgs) : rawArgs;
    const result = tool.schema.safeParse(preparedArgs);
    if (!result.success) {
      throw new ProtocolError(
        ProtocolErrorCode.InvalidParams,
        tool.formatValidationError?.(result.error) ?? formatSchemaValidationError(name, result.error),
      );
    }
    const strippedPaths = findStrippedPaths(preparedArgs, result.data);
    if (strippedPaths.length > 0) {
      throw new ProtocolError(
        ProtocolErrorCode.InvalidParams,
        `Invalid arguments for ${name}: unrecognized field(s): ${strippedPaths.join(", ")}`,
      );
    }
    return await tool.handler(result.data, client, extra);
  } catch (error: unknown) {
    throw handleToolError(error, name, args);
  }
}

/**
 * Guidance attached wherever a stale table id may be involved. Studio table ids
 * are derived from the table's content/position, so every successful edit gives
 * the edited table a NEW id and silently invalidates the old one. Without this
 * hint an agent reads the resulting 404 as "the edit was rolled back" and gives
 * up (EPBDS-16086) — the edit is in fact applied.
 */
export const STALE_TABLE_ID_HINT =
  "Table ids are derived from the table's location and change when an edit relocates the table " +
  "(it had no room to grow in place), so an id obtained before such an edit becomes stale while the " +
  "edit itself remains applied (a 404 here does NOT mean the edit was rolled back). Use the 'tableId' " +
  "returned by the last openl_update_table/openl_append_table response, or refresh ids with openl_list_tables().";

/**
 * Rethrow an HTTP 409 (conflict) from a mutating call as a clear, actionable
 * ProtocolError; rethrow anything else unchanged so it reaches {@link handleToolError}.
 *
 * The default status→ProtocolErrorCode mapping turns 409 into InternalError, which reads
 * to the model as a server fault rather than a recoverable "name already taken".
 * Create/clone use this to tell the model exactly how to recover.
 *
 * @returns never — always throws.
 */
export function rethrowConflictAsActionable(error: unknown, conflictMessage: string): never {
  if (isAxiosError(error) && error.response?.status === 409) {
    throw new ProtocolError(ProtocolErrorCode.InvalidRequest, conflictMessage);
  }
  throw error;
}

/** Caps how many field/global error lines are folded into one error message. */
const MAX_ERROR_LINES = 20;

/** Short, single-line preview of a rejected field value for the error message. */
function previewRejectedValue(value: unknown): string {
  // `value` always comes from a parsed JSON response (the caller only passes a
  // defined `rejectedValue`), so JSON.stringify yields a string here.
  const text = typeof value === "string" ? value : JSON.stringify(value);
  // Collapse whitespace on a bounded slice so a large rejected value (e.g. a big
  // cells block echoed back) isn't regex-scanned in full just to keep ~80 chars.
  const preview = text.slice(0, 80).replace(/\s+/g, " ").trim();
  return text.length > 80 ? `${preview}…` : preview;
}

/** Cap a list of error lines, appending a "… and N more" line when it overflows. */
function capErrorLines(lines: string[]): string[] {
  if (lines.length <= MAX_ERROR_LINES) {
    return lines;
  }
  return [...lines.slice(0, MAX_ERROR_LINES), `  - … and ${lines.length - MAX_ERROR_LINES} more`];
}

/**
 * Render a backend `ValidationError`'s per-field (`fields`) and additional global
 * (`errors`) entries as readable, indented lines for the agent-facing message.
 *
 * The studio returns these alongside a generic top-level message (e.g.
 * "Validation failed"); without them the agent only sees the generic line and
 * can't tell WHICH field failed or why. Returns "" when there is nothing to add.
 */
function formatApiErrorDetails(info: ExtractedErrorInfo): string {
  const sections: string[] = [];

  if (info.fields && info.fields.length > 0) {
    const lines = info.fields.map((f) => {
      const where = f.field ? `${f.field}: ` : "";
      const what = f.message || f.code || "invalid value";
      const rejected =
        f.rejectedValue !== undefined ? ` (rejected: ${previewRejectedValue(sanitizeJson(f.rejectedValue))})` : "";
      return `  - ${where}${what}${rejected}`;
    });
    sections.push(`Field errors:\n${capErrorLines(lines).join("\n")}`);
  }

  if (info.errors && info.errors.length > 0) {
    const lines = info.errors
      .map((e) => e.message || e.code)
      .filter((m): m is string => Boolean(m))
      .map((m) => `  - ${m}`);
    if (lines.length > 0) {
      sections.push(`Additional errors:\n${capErrorLines(lines).join("\n")}`);
    }
  }

  return sections.join("\n");
}

function handleToolError(error: unknown, toolName: string, toolArgs?: unknown): ProtocolError {
  // Enhanced error handling with context
  if (isAxiosError(error)) {
    const status = error.response?.status;
    const responseData = error.response?.data;
    const endpoint = error.config?.url;
    const method = error.config?.method ? error.config.method.toUpperCase() : undefined;
    const requestParams = error.config?.params; // Query parameters for GET requests
    const requestData = error.config?.data; // Request body for POST/PUT requests
    const axiosCode = error.code; // e.g. ECONNREFUSED, ETIMEDOUT, ENOTFOUND (network errors when no response)

    // Extract structured error information from API response
    const apiErrorInfo = extractApiErrorInfo(responseData, status);

    // Build error message with priority:
    // 1. API error message (if available)
    // 2. Field errors (for 400)
    // 3. Generic errors array (for 400)
    // 4. For network errors (no response): use code + message so we don't get just "Error"
    // 5. Fallback to sanitized axios error message
    let errorMessage = "";
    const errorDetails: Record<string, unknown> = {
      status,
      endpoint,
      method,
      tool: toolName,
    };
    if (axiosCode) {
      errorDetails.code = axiosCode;
    }

    // Add tool arguments (sanitized to prevent sensitive data exposure)
    if (toolArgs !== undefined) {
      errorDetails.toolArgs = sanitizeJson(toolArgs);
    }

    // Add request parameters (query params for GET requests)
    if (requestParams !== undefined && Object.keys(requestParams).length > 0) {
      errorDetails.requestParams = sanitizeJson(requestParams);
    }

    // Add request data (body for POST/PUT requests, sanitized)
    if (requestData !== undefined) {
      // Try to parse JSON if it's a string
      let parsedData = requestData;
      if (typeof requestData === "string") {
        try {
          parsedData = JSON.parse(requestData);
        } catch {
          // If parsing fails, use original string (will be sanitized as string)
          parsedData = requestData;
        }
      }
      errorDetails.requestData = sanitizeJson(parsedData);
    }

    // Add structured error information to details
    if (apiErrorInfo.code) {
      errorDetails.apiErrorCode = apiErrorInfo.code;
    }
    // Headline: the localized top-level message when present.
    if (apiErrorInfo.message) {
      errorMessage = apiErrorInfo.message;
    }
    // Keep the structured field/global errors for the server-side log, sanitized
    // the same way as toolArgs/requestData so a backend-echoed rejectedValue can't
    // leak a sensitive value into the log.
    if (apiErrorInfo.errors && apiErrorInfo.errors.length > 0) {
      errorDetails.errors = sanitizeJson(apiErrorInfo.errors);
    }
    if (apiErrorInfo.fields && apiErrorInfo.fields.length > 0) {
      errorDetails.fields = sanitizeJson(apiErrorInfo.fields);
    }
    // ...and ALWAYS fold them into the agent-facing message (not only when there
    // is no top-level message), so a ValidationError's specifics — which field
    // failed, why, and the rejected value — survive instead of being hidden
    // behind a generic "Validation failed".
    const validationDetails = formatApiErrorDetails(apiErrorInfo);
    if (validationDetails) {
      errorMessage = errorMessage ? `${errorMessage}\n${validationDetails}` : validationDetails;
    }
    if (apiErrorInfo.rawResponse && !apiErrorInfo.code && !apiErrorInfo.message) {
      // Unknown format - include raw response in details
      errorDetails.rawResponse = apiErrorInfo.rawResponse;
    }

    // Fallback to sanitized axios error message if no API message
    if (!errorMessage) {
      const sanitized = sanitizeError(error);
      // For network errors (axiosCode set, no response), always include code so the cause is visible
      errorMessage = axiosCode ? `${axiosCode}: ${sanitized}` : sanitized;
    }

    // Build final error message. The studio's REST method/endpoint are kept in
    // errorDetails for the server-side log only — they are deliberately NOT put in
    // the message, which reaches the calling agent (as an isError result): the
    // agent acts on tools, not raw API paths, so exposing the backend endpoint adds
    // noise and leaks internal API shape.
    let finalMessage = `OpenL Studio API error`;
    if (status) {
      finalMessage += ` (${status})`;
    }
    finalMessage += `: ${errorMessage}`;

    // EPBDS-16086: a bare "The table is not found" after an edit reads as a
    // rollback. Explain that table ids go stale on every edit and how to recover.
    if (status === 404 && typeof endpoint === "string" && /\/tables\/[^/?]+/.test(endpoint)) {
      finalMessage += ` Hint: ${STALE_TABLE_ID_HINT}`;
    }

    // Log one-line summary first (status or network code + message) so it's visible at a glance in VS Code/Copilot output
    const summary =
      status != null
        ? `${toolName} (${status}) ${errorMessage}`
        : axiosCode
          ? `${toolName} [${axiosCode}] ${errorMessage}`
          : `${toolName} ${errorMessage}`;
    logger.error(`Tool error: ${summary}`, errorDetails);

    // Use appropriate error code based on status
    let errorCode = ProtocolErrorCode.InternalError;
    if (status === 400) {
      errorCode = ProtocolErrorCode.InvalidParams;
    } else if (status === 401 || status === 403) {
      errorCode = ProtocolErrorCode.InvalidRequest; // MCP doesn't have specific auth error code
    } else if (status === 404) {
      errorCode = ProtocolErrorCode.InvalidParams;
    } else if (status === 405) {
      errorCode = ProtocolErrorCode.MethodNotFound;
    }

    throw new ProtocolError(
      errorCode,
      finalMessage,
      errorDetails
    );
  }

  // Re-throw protocol errors as-is.
  if (error instanceof ProtocolError) {
    throw error;
  }

  // Wrap other errors with sanitization
  const sanitizedMessage = sanitizeError(error);
  const errorDetails: Record<string, unknown> = {
    tool: toolName,
    error: sanitizedMessage,
  };

  // Add tool arguments (sanitized to prevent sensitive data exposure)
  if (toolArgs !== undefined) {
    errorDetails.toolArgs = sanitizeJson(toolArgs);
  }

  logger.error(`Tool error: ${toolName} ${sanitizedMessage}`, errorDetails);

  throw new ProtocolError(
    ProtocolErrorCode.InternalError,
    `Error executing ${toolName}: ${sanitizedMessage}`,
    errorDetails
  );
}

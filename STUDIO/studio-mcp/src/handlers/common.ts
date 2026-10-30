/**
 * Shared core for the OpenL tool handlers.
 *
 * Holds the tool registry (the `registerTool` / `getAllTools` / `executeTool`
 * trio and the `ToolDefinition` shape), the answer of a tool, and the
 * cross-cutting error handling that every tool shares. The per-category handler
 * modules in this directory import from here and never the other way around, so
 * the registry lives in exactly one place.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { ProgressToken, ServerNotification, ToolAnnotations } from "@modelcontextprotocol/server";
import { z, type ZodType } from "zod";

import type { OpenLClient } from "../client.js";
import type { ResponseFormat } from "../types.js";
import { sanitizeError, extractApiErrorInfo, sanitizeJson, type ExtractedErrorInfo } from "../utils.js";
import { isHttpError, type HttpError } from "../http.js";
import { formatResponse, type FormatOptions } from "../formatters.js";
import { refreshWhenChangedOnDisk } from "./freshness.js";

/**
 * Tool response structure
 */
export interface ToolResponse {
  [key: string]: unknown;
  // Every OpenL result is text, which every MCP client can read: the answer in the first block, and a fact about a
  // verbatim answer, such as the version of a file read inline, in a block of its own
  content: [{ type: "text"; text: string }, ...{ type: "text"; text: string }[]];
}

/** The answer of a tool made of one text block. */
export function textResult(text: string): ToolResponse {
  return { content: [{ type: "text", text }] };
}

/** The answer of a tool that renders `data` in the format the caller asked for. */
export function formatResult(data: unknown, format?: ResponseFormat, options?: FormatOptions): ToolResponse {
  return textResult(formatResponse(data, format, options));
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

type ToolHandler<TSchema extends ZodType> = (
  args: z.output<TSchema>,
  client: OpenLClient,
  extra?: ToolHandlerExtra,
) => Promise<ToolResponse>;

interface ToolDefinition<TSchema extends ZodType = ZodType> {
  name: string;
  title: string;
  description: string;
  /**
   * Single source of truth for both the JSON Schema advertised over MCP and the
   * arguments accepted at runtime.
   */
  schema: TSchema;
  annotations?: ToolAnnotations;
  /**
   * Optional pre-schema coercion or domain-specific validation of raw arguments.
   * The returned value is always validated against `schema` before the handler
   * runs. Used by structured-payload table tools for JSON-string coercion and
   * richer discriminator errors.
   */
  validateArgs?: (args: unknown) => unknown;
  /** Optional tool-specific enrichment for schema validation failures. */
  formatValidationError?: (error: z.ZodError) => string;
  /**
   * Whether the tool works with what Studio compiled of the project its `projectId` names. When another program
   * changed the files of the project since Studio compiled it, the project is compiled again first, and the tool
   * goes on with the files as they are: an edit of a table changed since the agent read it is refused by the version
   * of the table it names. Left out for a tool that does not work with what was compiled.
   */
  compiled?: boolean;
  handler: ToolHandler<TSchema>;
}

const toolHandlers = new Map<string, ToolDefinition>();

/** The discovery hint for an identifier a caller got wrong, by the argument it names. */
const IDENTIFIER_HINTS: ReadonlyArray<[string, string]> = [
  ["projectId", "Use openl_list_projects() to find valid project IDs."],
  ["tableId", "Use openl_list_tables() to find valid table IDs."],
  ["repository", "Use openl_list_repositories() to find valid repositories."],
];

/**
 * Add discovery hints for identifiers rejected before a handler can run, and the arguments a tool takes when the
 * caller named one it does not.
 */
export function formatSchemaValidationError(name: string, error: z.ZodError): string {
  const message = `Invalid arguments for ${name}:\n${z.prettifyError(error)}`;
  const topLevelFields = new Set(error.issues.map((issue) => issue.path[0]));
  const hints = IDENTIFIER_HINTS.filter(([field]) => topLevelFields.has(field)).map(([, hint]) => hint);
  const taken = takenArguments(name, error);
  if (taken) hints.push(taken);
  return hints.length > 0 ? `${message}\n\n${hints.join(" ")}` : message;
}

/** The arguments of a tool, for a call that named an unknown one; nothing for one unknown inside another argument. */
function takenArguments(name: string, error: z.ZodError): string | undefined {
  const tool = toolHandlers.get(name);
  if (!tool || !error.issues.some((issue) => issue.code === "unrecognized_keys" && issue.path.length === 0)) {
    return undefined;
  }
  const properties = getInputSchema(tool.schema).properties as Record<string, unknown>;
  return `${name} takes only: ${Object.keys(properties).join(", ")}.`;
}

/**
 * The JSON Schema a tool publishes. Every agent loads it in every session, so it leaves out what tells the agent
 * nothing: the dialect, and the bounds of a safe integer Zod puts on every integer. `executeTool` still validates the
 * arguments with the Zod schema.
 */
function getInputSchema(schema: ZodType): Record<string, unknown> {
  // The input side: a field with a default may be omitted by the client.
  const inputSchema = z.toJSONSchema(schema, {
    io: "input",
    override: ({ jsonSchema }) => {
      if (jsonSchema.maximum === Number.MAX_SAFE_INTEGER) delete jsonSchema.maximum;
      if (jsonSchema.minimum === Number.MIN_SAFE_INTEGER) delete jsonSchema.minimum;
    },
  }) as Record<string, unknown>;
  delete inputSchema.$schema;
  // MCP tool input schemas always have an object root. Zod emits object-only
  // unions as a top-level anyOf without repeating that shared root type.
  inputSchema.type ??= "object";
  return inputSchema;
}

export function registerTool<TSchema extends ZodType>(tool: ToolDefinition<TSchema>): void {
  // The schema/handler pair is type-checked together at registration. The map
  // erases that generic relationship, then executeTool restores it by parsing
  // with the stored schema before invoking the stored handler.
  toolHandlers.set(tool.name, tool as ToolDefinition);
}

/** The registered tools as the MCP tool list advertises them. */
export function getAllTools(): Array<
  Pick<ToolDefinition, "name" | "title" | "description" | "annotations"> & { inputSchema: Record<string, unknown> }
> {
  return Array.from(toolHandlers.values(), ({ name, title, description, annotations, schema }) => ({
    name,
    title,
    description,
    annotations,
    inputSchema: getInputSchema(schema),
  }));
}

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
    await keepCurrent(tool, result.data, client, extra);
    return await tool.handler(result.data, client, extra);
  } catch (error: unknown) {
    throw isChangedOutside(error)
      ? new ProtocolError(ProtocolErrorCode.InvalidRequest, CHANGED_OUTSIDE_MESSAGE)
      : handleToolError(error, name, args);
  }
}

/** The code OpenL Studio refuses a write with when another program changed the workbook since it was read. */
const CHANGED_OUTSIDE_CODE = "openl.error.409.workbook.changed.outside.message";

/**
 * What the agent does after such a refusal. Studio asks a person to refresh the project, but an agent has no such
 * call: Studio reads the files again by itself after the refusal.
 */
const CHANGED_OUTSIDE_MESSAGE =
  "A workbook of the project changed outside OpenL Studio, such as in Excel, since it was read, and nothing was written. "
  + "OpenL Studio reads the project files again by itself, so no refresh is needed: read the table again with "
  + "openl_get_table and repeat the edit on what it holds now.";

/** Whether Studio refused a write because another program changed the workbook since it was read. */
function isChangedOutside(error: unknown): boolean {
  return isHttpError(error) && error.response?.status === 409
    && extractApiErrorInfo(error.response.data, 409).code === CHANGED_OUTSIDE_CODE;
}

/** Compiles the project a tool works with again when its files changed on disk; see {@link ToolDefinition.compiled}. */
async function keepCurrent(
  tool: ToolDefinition,
  args: unknown,
  client: OpenLClient,
  extra?: ToolHandlerExtra,
): Promise<void> {
  const projectId = (args as { projectId?: unknown }).projectId;
  if (!tool.compiled || typeof projectId !== "string") {
    return;
  }
  await refreshWhenChangedOnDisk(client, projectId, extra?.signal);
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

/**
 * Writes a failed tool call to standard error, which OpenL Studio keeps in its `org.openl.studio.mcp.node` logger: the
 * summary, then the sanitized details. The details name the REST address the call failed on, which the agent never
 * gets.
 */
function logToolError(summary: string, details: Record<string, unknown>): void {
  console.error(`[ERROR] Tool error: ${summary}`, JSON.stringify(details));
}

/** The protocol error of an HTTP status the agent can act on; any other status is an internal error. */
const ERROR_CODE_OF_STATUS: ReadonlyMap<number, ProtocolErrorCode> = new Map([
  [400, ProtocolErrorCode.InvalidParams],
  // MCP has no error code of its own for authentication
  [401, ProtocolErrorCode.InvalidRequest],
  [403, ProtocolErrorCode.InvalidRequest],
  [404, ProtocolErrorCode.InvalidParams],
  [405, ProtocolErrorCode.MethodNotFound],
]);

/** The arguments of a tool for the log, sanitized so that no sensitive value reaches it. */
function sanitizedArgs(toolArgs: unknown): Record<string, unknown> {
  return toolArgs === undefined ? {} : { toolArgs: sanitizeJson(toolArgs) };
}

/** The details of a failed REST call for the log: where it went, what it sent and what OpenL Studio answered. */
function httpErrorDetails(
  error: HttpError,
  toolName: string,
  toolArgs: unknown,
  apiErrorInfo: ExtractedErrorInfo,
): Record<string, unknown> {
  const { params, data } = error.config;
  return {
    status: error.response?.status,
    endpoint: error.config.url,
    method: error.config.method.toUpperCase(),
    tool: toolName,
    ...sanitizedArgs(toolArgs),
    // The system error of a failure without an answer, e.g. ECONNREFUSED, ENOTFOUND
    ...(error.code ? { code: error.code } : {}),
    ...(params !== undefined && Object.keys(params).length > 0 ? { requestParams: sanitizeJson(params) } : {}),
    // A JSON body is the tool arguments again; any other body, such as the bytes of a file, is logged by its size
    ...(data !== undefined && typeof data !== "string" ? { requestData: sanitizeJson(data) } : {}),
    ...(apiErrorInfo.code ? { apiErrorCode: apiErrorInfo.code } : {}),
    // The field and global errors, sanitized like the arguments, so that a rejected value OpenL Studio echoes cannot
    // leak a sensitive value into the log
    ...(apiErrorInfo.errors?.length ? { errors: sanitizeJson(apiErrorInfo.errors) } : {}),
    ...(apiErrorInfo.fields?.length ? { fields: sanitizeJson(apiErrorInfo.fields) } : {}),
    // An answer of an unknown shape
    ...(apiErrorInfo.rawResponse ? { rawResponse: apiErrorInfo.rawResponse } : {}),
  };
}

/**
 * The message of a failed REST call: the localized message OpenL Studio answered, followed by its field and global
 * errors, so that a validation error names the field that failed, why, and the rejected value. A call without such an
 * answer reports its sanitized error, with the system error of a failure without an answer.
 */
function httpErrorMessage(error: HttpError, apiErrorInfo: ExtractedErrorInfo): string {
  const answered = [apiErrorInfo.message, formatApiErrorDetails(apiErrorInfo)].filter(Boolean).join("\n");
  if (answered) {
    return answered;
  }
  const sanitized = sanitizeError(error);
  return error.code ? `${error.code}: ${sanitized}` : sanitized;
}

/** The one-line summary a failed REST call is logged with: the tool, the status or the system error, the message. */
function httpErrorSummary(toolName: string, error: HttpError, message: string): string {
  const status = error.response?.status;
  if (status != null) {
    return `${toolName} (${status}) ${message}`;
  }
  return error.code ? `${toolName} [${error.code}] ${message}` : `${toolName} ${message}`;
}

/**
 * The error of a failed REST call. The method and the address of the call are logged only: they never reach the
 * agent, which acts on tools, not on the REST API of OpenL Studio.
 */
function httpToolError(error: HttpError, toolName: string, toolArgs: unknown): ProtocolError {
  const status = error.response?.status;
  const apiErrorInfo = extractApiErrorInfo(error.response?.data, status);
  const details = httpErrorDetails(error, toolName, toolArgs, apiErrorInfo);
  const message = httpErrorMessage(error, apiErrorInfo);
  logToolError(httpErrorSummary(toolName, error, message), details);

  const statusText = status ? ` (${status})` : "";
  const errorCode = ERROR_CODE_OF_STATUS.get(status ?? 0) ?? ProtocolErrorCode.InternalError;
  return new ProtocolError(errorCode, `OpenL Studio API error${statusText}: ${message}`, details);
}

/** The error a failed tool call reports: a `ProtocolError` with a sanitized message, logged with its details. */
function handleToolError(error: unknown, toolName: string, toolArgs?: unknown): ProtocolError {
  if (isHttpError(error)) {
    return httpToolError(error, toolName, toolArgs);
  }

  // Protocol errors pass as they are.
  if (error instanceof ProtocolError) {
    return error;
  }

  // Any other error, sanitized
  const sanitizedMessage = sanitizeError(error);
  const errorDetails: Record<string, unknown> = {
    tool: toolName,
    error: sanitizedMessage,
    ...sanitizedArgs(toolArgs),
  };
  logToolError(`${toolName} ${sanitizedMessage}`, errorDetails);

  return new ProtocolError(
    ProtocolErrorCode.InternalError,
    `Error executing ${toolName}: ${sanitizedMessage}`,
    errorDetails
  );
}

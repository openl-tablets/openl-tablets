/**
 * OpenL MCP Server — CLI mode
 *
 * Lets users invoke any registered `openl_*` tool directly from the shell
 * without an MCP client. Reuses the existing `executeTool` registry from
 * `src/handlers/`, so every tool the MCP server exposes is available here
 * with the same input schemas, validation, and response formatting.
 *
 * Usage:
 *   npx -y openl-mcp <tool-name> [<json-args> | @file.json | --stdin] [flags]
 *   npx -y openl-mcp --help
 *   npx -y openl-mcp --list-tools
 *
 * Config (env vars, can be overridden by CLI flags):
 *   OPENL_BASE_URL                (required)  → --base-url <url>
 *   OPENL_PERSONAL_ACCESS_TOKEN   (auth)      → --token <pat>
 *   OPENL_TIMEOUT                 (optional)  → --timeout <ms>
 */

import { readFile, writeFile } from "node:fs/promises";

import { OpenLClient } from "./client.js";
import { versionInfo, versionLine } from "./build-info.js";
import { SERVER_INFO, TOOL_CATEGORIES } from "./constants.js";
import { executeTool, getAllTools, registerAllTools } from "./handlers/index.js";
import { hashFingerprint, sanitizeError, normalizeToken } from "./utils.js";
import type * as Types from "./types.js";

/**
 * Exit codes following BSD `sysexits.h` conventions. Lets CI/CD pipelines
 * distinguish "bad args, don't retry" from "API down, retry might work".
 *
 * Reference: https://man7.org/linux/man-pages/man3/sysexits.h.3head.html
 */
export const EXIT_CODES = {
  /** Successful execution. */
  OK: 0,
  /** Unclassified failure. */
  GENERIC: 1,
  /** Bad CLI arguments / flag usage. */
  USAGE: 64,
  /** Bad input data (e.g. malformed JSON in args). */
  DATAERR: 65,
  /** Service unavailable (ECONNREFUSED, ETIMEDOUT, DNS failure). */
  UNAVAILABLE: 69,
  /** Authentication / authorization failure (401, 403). */
  NOPERM: 77,
  /** Missing or invalid configuration. */
  CONFIG: 78,
} as const;

/**
 * Custom error carrying an explicit exit code. Thrown from CLI-level code
 * (config building, argument resolution) to surface a precise classification
 * up to the top-level catch in `runCli`.
 */
export class CliError extends Error {
  constructor(message: string, public readonly exitCode: number) {
    super(message);
    this.name = "CliError";
  }
}

/**
 * Classify an error from `executeTool` (or any unknown source) into a
 * `sysexits.h` exit code, by inspecting message patterns / error codes.
 *
 * The OpenL client wraps API errors as `MCP error -326xx: ... (HTTP STATUS)
 * [METHOD /path]` — we extract status via regex. Network failures (axios
 * `error.code`) bubble up as `ECONNREFUSED`/`ETIMEDOUT`/`ENOTFOUND` in the
 * message. Anything we can't classify falls back to GENERIC.
 */
export function classifyError(error: unknown): number {
  if (error instanceof CliError) return error.exitCode;
  const msg = error instanceof Error ? error.message : String(error);

  // Network / connectivity → UNAVAILABLE
  if (/\b(ECONNREFUSED|ETIMEDOUT|ENOTFOUND|EAI_AGAIN|ECONNRESET|EHOSTUNREACH|ENETUNREACH)\b/.test(msg)) {
    return EXIT_CODES.UNAVAILABLE;
  }

  // Extract the HTTP status code and classify by it. The OpenL client wraps
  // API errors as "OpenL Studio API error (NNN): ..."; axios also emits
  // "Request failed with status code NNN". Match whichever form is present.
  const statusMatch = msg.match(/\((\d{3})\)/) ?? msg.match(/status code (\d{3})/i);
  if (statusMatch) {
    const status = Number(statusMatch[1]);
    if (status === 401 || status === 403) return EXIT_CODES.NOPERM; // auth/permission
    if (status >= 500 && status <= 599) return EXIT_CODES.UNAVAILABLE; // server down/unhealthy
  }
  return EXIT_CODES.GENERIC;
}

/**
 * Options for `runCli`. Stdio and env are injectable for testability.
 */
interface RunCliOptions {
  /** Args after `node script.js` (i.e. `process.argv.slice(2)`). */
  argv: string[];
  /** Environment variables. Defaults to `process.env`. */
  env?: NodeJS.ProcessEnv;
  /** Readable stream for `--stdin`. Defaults to `process.stdin`. */
  stdin?: NodeJS.ReadableStream;
  /** Where to write tool output. Defaults to `process.stdout`. */
  stdout?: NodeJS.WritableStream;
  /** Where to write diagnostics and errors. Defaults to `process.stderr`. */
  stderr?: NodeJS.WritableStream;
  /** Inject a custom client (for tests). */
  clientFactory?: (config: Types.OpenLConfig) => OpenLClient;
}

/** Parsed shape of CLI arguments. */
export interface ParsedArgs {
  toolName?: string;
  /**
   * A bareword positional that parses as an `http(s)://` URL. Tool names are
   * never URLs, so a leading URL is unambiguously the OpenL base URL — used to
   * launch the MCP server (`openl-mcp <url>`) or as a positional override for a
   * tool call (`openl-mcp <url> <tool>`). Resolution precedence for the base
   * URL is: this positional → `--base-url` flag → `OPENL_BASE_URL` env.
   */
  baseUrlPositional?: string;
  inlineJson?: string;
  fileArg?: string;
  useStdin: boolean;
  showHelp: boolean;
  showVersion: boolean;
  listTools: boolean;
  cookieJarPath?: string;
  overrides: {
    baseUrl?: string;
    token?: string;
    timeout?: number;
  };
  errors: string[];
}

/**
 * True when `value` parses as an absolute `http`/`https` URL. Used to tell a
 * positional base URL apart from a tool name during argument parsing — tool
 * names (e.g. `openl_list_repositories`) never parse as URLs.
 */
function looksLikeHttpUrl(value: string): boolean {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return false;
  }
  return url.protocol === "http:" || url.protocol === "https:";
}

/**
 * Parse argv into a `ParsedArgs` record. No I/O. No throwing — errors are
 * collected and returned for the caller to render.
 */
export function parseArgs(argv: string[]): ParsedArgs {
  const result: ParsedArgs = {
    useStdin: false,
    showHelp: false,
    showVersion: false,
    listTools: false,
    overrides: {},
    errors: [],
  };

  const takeValue = (i: number, flag: string): string | undefined => {
    const value = argv[i + 1];
    if (value === undefined || value.startsWith("--")) {
      result.errors.push(`Flag ${flag} requires a value`);
      return undefined;
    }
    return value;
  };

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];

    switch (arg) {
      case "-h":
      case "--help":
        result.showHelp = true;
        break;
      case "-V":
      case "--version":
        result.showVersion = true;
        break;
      case "--list-tools":
        result.listTools = true;
        break;
      case "--stdin":
        result.useStdin = true;
        break;
      case "--base-url":
        result.overrides.baseUrl = takeValue(i, arg);
        i++;
        break;
      case "--token":
        result.overrides.token = takeValue(i, arg);
        i++;
        break;
      case "--timeout": {
        const raw = takeValue(i, arg);
        i++;
        if (raw !== undefined) {
          const parsed = Number.parseInt(raw, 10);
          if (Number.isNaN(parsed) || parsed <= 0) {
            result.errors.push(`Invalid --timeout value: ${raw}`);
          } else {
            result.overrides.timeout = parsed;
          }
        }
        break;
      }
      case "--cookie-jar":
        result.cookieJarPath = takeValue(i, arg);
        i++;
        break;
      default:
        if (arg.startsWith("@")) {
          if (result.fileArg) {
            result.errors.push(`Multiple @file arguments are not allowed`);
          } else {
            result.fileArg = arg.slice(1);
          }
        } else if (arg.startsWith("{") || arg.startsWith("[")) {
          if (result.inlineJson) {
            result.errors.push(`Multiple inline JSON arguments are not allowed`);
          } else {
            result.inlineJson = arg;
          }
        } else if (arg.startsWith("--")) {
          result.errors.push(`Unknown flag: ${arg}`);
        } else if (looksLikeHttpUrl(arg)) {
          // A bareword http(s) URL is the OpenL base URL, not a tool name
          // (tool names never parse as URLs). Accepted in any position so
          // both `openl-mcp <url> <tool>` and `openl-mcp <tool> <url>` work.
          // A second URL is a user error — report it directly (mirroring the
          // @file / inline-JSON duplicate checks) rather than letting it slip
          // into the tool-name slot and mis-blame the real tool name.
          if (result.baseUrlPositional === undefined) {
            result.baseUrlPositional = arg;
          } else {
            result.errors.push(`Multiple base URLs are not allowed: ${arg}`);
          }
        } else if (!result.toolName) {
          result.toolName = arg;
        } else {
          result.errors.push(`Unexpected positional argument: ${arg}`);
        }
        break;
    }
  }

  // Source of args is mutually exclusive
  const sources = [result.inlineJson, result.fileArg, result.useStdin ? "stdin" : undefined].filter(Boolean);
  if (sources.length > 1) {
    result.errors.push(`Provide tool arguments via only one of: inline JSON, @file, or --stdin`);
  }

  return result;
}

/**
 * Decide whether a parsed argv represents a CLI/tool invocation (run one tool,
 * print help/version, list tools, or report a usage error) as opposed to an
 * MCP server launch.
 *
 * Used by the binary's entry point (`src/index.ts`) to route: anything with a
 * tool name, a discovery flag, a tool-argument source, or a parse error goes
 * to `runCli`; everything else (no args, or just a positional `<url>` and/or
 * server flags) starts the stdio MCP server.
 *
 * A bare positional `<url>` is deliberately NOT a CLI signal — that is the new
 * `openl-mcp <url>` server-launch form.
 */
export function isCliInvocation(parsed: ParsedArgs): boolean {
  return (
    parsed.toolName !== undefined ||
    parsed.showHelp ||
    parsed.showVersion ||
    parsed.listTools ||
    parsed.inlineJson !== undefined ||
    parsed.fileArg !== undefined ||
    parsed.useStdin ||
    parsed.errors.length > 0
  );
}

/**
 * Read raw text from a stream until EOF. Used for `--stdin` JSON input.
 */
async function readStream(stream: NodeJS.ReadableStream): Promise<string> {
  const chunks: Buffer[] = [];
  for await (const chunk of stream) {
    chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
  }
  return Buffer.concat(chunks).toString("utf-8");
}

/**
 * Resolve tool arguments from inline JSON / @file / --stdin / nothing.
 * Returns the parsed value or `undefined` if no args were provided.
 */
async function resolveToolArgs(
  parsed: ParsedArgs,
  stdin: NodeJS.ReadableStream,
): Promise<unknown> {
  let raw: string | undefined;

  if (parsed.inlineJson) {
    raw = parsed.inlineJson;
  } else if (parsed.fileArg) {
    raw = await readFile(parsed.fileArg, "utf-8");
  } else if (parsed.useStdin) {
    raw = await readStream(stdin);
  }

  if (raw === undefined || raw.trim() === "") {
    return undefined;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    throw new Error(`Failed to parse tool arguments as JSON: ${(error as Error).message}`);
  }
}

/**
 * Build an `OpenLConfig` from env + CLI overrides. Requires a valid base URL;
 * authentication is optional — a token (from `--token` or
 * `OPENL_PERSONAL_ACCESS_TOKEN`) is used when present, and without one the
 * client sends no Authorization header (OpenL Studio single-user mode).
 */
function buildConfig(
  env: NodeJS.ProcessEnv,
  overrides: ParsedArgs["overrides"],
): Types.OpenLConfig {
  const baseUrl = overrides.baseUrl ?? env.OPENL_BASE_URL;
  if (!baseUrl) {
    throw new Error(
      "OPENL_BASE_URL is required (set the env var or pass --base-url <url>)",
    );
  }
  try {
    new URL(baseUrl);
  } catch {
    throw new Error(`Invalid base URL: ${baseUrl}`);
  }

  // Authentication is optional, matching the stdio server: not supplying a
  // token simply means an unauthenticated (anonymous) request. There is no
  // separate flag for that — it is the absence of `--token`. A blank/whitespace
  // token is treated as absent (via normalizeToken) so it never sends an empty
  // credential.
  const personalAccessToken = normalizeToken(overrides.token ?? env.OPENL_PERSONAL_ACCESS_TOKEN);

  let timeout = overrides.timeout;
  if (timeout === undefined && env.OPENL_TIMEOUT) {
    const parsed = Number.parseInt(env.OPENL_TIMEOUT, 10);
    if (Number.isNaN(parsed) || parsed <= 0) {
      throw new Error(`Invalid OPENL_TIMEOUT value: ${env.OPENL_TIMEOUT}`);
    }
    timeout = parsed;
  }

  return { baseUrl, personalAccessToken, timeout };
}

/**
 * A cookie-jar entry binds a persisted JSESSIONID to the server and principal
 * it was issued for, so it is never replayed across a different host or user.
 */
interface CookieJarData {
  /** Normalized studio base URL the session belongs to (client.getBaseUrl()). */
  baseUrl: string;
  /** sha256 fingerprint of the Authorization header (the principal). */
  authFingerprint: string;
  /** The JSESSIONID value. */
  jsessionId: string;
}

/**
 * Studio session ids are opaque alphanumerics; reject anything else so a
 * tampered jar can't inject extra Cookie-header directives when replayed.
 */
function isValidJSessionId(value: string): boolean {
  return /^[A-Za-z0-9._-]+$/.test(value);
}

/**
 * Compute the binding (server + principal) for the current invocation. A
 * persisted JSESSIONID is only restored when its stored binding matches this.
 */
function computeCookieBinding(client: OpenLClient): { baseUrl: string; authFingerprint: string } {
  return {
    baseUrl: client.getBaseUrl(),
    authFingerprint: hashFingerprint(client.getAuthorizationHeader() ?? "anonymous"),
  };
}

/**
 * Load a previously persisted JSESSIONID from a cookie-jar file — but only when
 * it was saved for the SAME base URL and principal as the current invocation,
 * so a session never leaks across hosts or users.
 *
 * Returns `null` (fresh session) for a missing/unreadable file, the legacy
 * bare-cookie format (no binding to verify), a binding mismatch, or an invalid
 * session id. Non-fatal cases emit a stderr warning so the user notices.
 */
async function loadCookieJar(
  path: string,
  binding: { baseUrl: string; authFingerprint: string },
  stderr: NodeJS.WritableStream,
): Promise<string | null> {
  let raw: string;
  try {
    raw = await readFile(path, "utf-8");
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    if (code === "ENOENT") return null; // first run — no jar yet
    stderr.write(
      `Warning: could not read cookie jar at ${path}: ${(error as Error).message}\n`,
    );
    return null;
  }

  const trimmed = raw.trim();
  if (trimmed === "") return null;

  let data: Partial<CookieJarData> | null = null;
  try {
    const parsed: unknown = JSON.parse(trimmed);
    if (parsed && typeof parsed === "object") {
      data = parsed as Partial<CookieJarData>;
    }
  } catch {
    // Legacy format (a bare JSESSIONID line) carried no server/principal
    // binding, so we cannot verify it belongs here — discard rather than replay.
    stderr.write(
      `Warning: ignoring legacy cookie jar at ${path} (no server/user binding); starting a fresh session.\n`,
    );
    return null;
  }

  if (!data || typeof data.jsessionId !== "string" || !isValidJSessionId(data.jsessionId)) {
    stderr.write(
      `Warning: ignoring malformed cookie jar at ${path}; starting a fresh session.\n`,
    );
    return null;
  }
  if (data.baseUrl !== binding.baseUrl || data.authFingerprint !== binding.authFingerprint) {
    stderr.write(
      `Warning: ignoring cookie jar at ${path}: it was saved for a different server or user; starting a fresh session.\n`,
    );
    return null;
  }
  return data.jsessionId;
}

/**
 * Persist the client's current JSESSIONID to the cookie-jar file with 0600
 * permissions, bound to the current base URL and principal so a later run only
 * replays it for the same server+user. Best-effort: write failures emit a
 * warning but do not fail the tool invocation.
 *
 * When the client has no session cookie (e.g. the tool hit only stateless
 * endpoints), no file is written.
 */
async function saveCookieJar(
  path: string,
  cookie: string | null,
  binding: { baseUrl: string; authFingerprint: string },
  stderr: NodeJS.WritableStream,
): Promise<void> {
  if (cookie === null) return;
  if (!isValidJSessionId(cookie)) {
    stderr.write(
      `Warning: not persisting cookie jar at ${path}: unexpected session id format.\n`,
    );
    return;
  }
  const data: CookieJarData = {
    baseUrl: binding.baseUrl,
    authFingerprint: binding.authFingerprint,
    jsessionId: cookie,
  };
  try {
    await writeFile(path, `${JSON.stringify(data)}\n`, { mode: 0o600 });
  } catch (error) {
    stderr.write(
      `Warning: could not write cookie jar at ${path}: ${(error as Error).message}\n`,
    );
  }
}

/**
 * Suppress informational stderr logs from the OpenL client / auth manager
 * (e.g. `[Auth] 🔐 PAT Authentication ...`) while the CLI is running.
 *
 * These are useful in MCP-stdio mode but pollute shell pipelines. We flip
 * `OPENL_CLI_QUIET=1` on `process.env` (which the auth manager reads
 * directly via `parseBoolEnv`); the returned closure restores the prior
 * value so callers can clean up after themselves. Genuine error logs are
 * never gated by this flag.
 *
 * Note: only `process.env` matters here — the injected `env` in
 * `RunCliOptions` is for our own config reads (base URL, credentials),
 * but `auth.ts` looks at the real `process.env` because it can't see
 * our `RunCliOptions`. So we mutate the real env, not the injected one.
 */
function setQuietMode(): () => void {
  const prev = process.env.OPENL_CLI_QUIET;
  process.env.OPENL_CLI_QUIET = "1";
  return () => {
    if (prev === undefined) delete process.env.OPENL_CLI_QUIET;
    else process.env.OPENL_CLI_QUIET = prev;
  };
}

/**
 * Resolve a user-typed CLI tool name to a registered name, or `undefined` when
 * nothing matches.
 *
 * The registry uses bare names — the `openl_` prefix is an MCP-protocol concern
 * added only on the wire — so the CLI matches directly: `list_repositories`
 * resolves, while the fully-qualified `openl_…` wire form is not a CLI command
 * and simply isn't found.
 */
function resolveToolName(input: string): string | undefined {
  return getAllTools().some((t) => t.name === input) ? input : undefined;
}

/**
 * Render help for a specific tool: title, description, and a human-readable
 * summary of its JSON Schema (properties, required, types, descriptions).
 * Returns `null` if the tool isn't registered.
 */
function renderToolHelp(toolName: string): string | null {
  const tool = getAllTools().find((t) => t.name === toolName);
  if (!tool) return null;

  const schema = tool.inputSchema as {
    type?: string;
    properties?: Record<string, { type?: string; description?: string; enum?: unknown[]; default?: unknown }>;
    required?: string[];
  };
  const required = new Set(schema.required ?? []);
  const props = schema.properties ?? {};

  const propLines = Object.entries(props).map(([name, def]) => {
    const flag = required.has(name) ? "required" : "optional";
    const type = def.type ?? "any";
    const enumPart = def.enum ? ` (one of: ${def.enum.join(", ")})` : "";
    const desc = def.description ? `\n      ${def.description}` : "";
    return `  ${name.padEnd(24)} ${type.padEnd(8)} [${flag}]${enumPart}${desc}`;
  });

  return [
    `${tool.name}`,
    tool.title ? `  ${tool.title}` : "",
    "",
    "Description:",
    `  ${tool.description}`,
    "",
    "Arguments:",
    propLines.length > 0 ? propLines.join("\n") : "  (none)",
    "",
    "Example:",
    `  npx -y openl-mcp ${tool.name} '{}'`,
    `  npx -y openl-mcp ${tool.name} @args.json`,
    `  echo '{...}' | npx -y openl-mcp ${tool.name} --stdin`,
    "",
    `Full JSON Schema: npx -y openl-mcp --list-tools | jq '.[] | select(.name=="${tool.name}")'`,
    "",
  ].filter((line) => line !== "").join("\n") + "\n";
}

/**
 * Render help text listing usage and every registered tool, grouped by
 * category for readability (Repository / Project / Rules / Trace / ...).
 */
function renderHelp(): string {
  // Group tools by category, then render each section. The listing shows the
  // human-readable title (not the description) so the catalog stays scannable;
  // `<tool> --help` gives the full description + schema, and `--list-tools`
  // gives the machine-readable JSON.
  const byCategory = new Map<string, Array<{ name: string; title: string }>>();
  for (const t of getAllTools()) {
    let bucket = byCategory.get(t.category);
    if (!bucket) {
      bucket = [];
      byCategory.set(t.category, bucket);
    }
    // Registry names are already bare — that's exactly what you type on the CLI.
    bucket.push({ name: t.name, title: t.title ?? "" });
  }
  const sections: string[] = [];
  for (const cat of TOOL_CATEGORIES) {
    const items = byCategory.get(cat);
    if (!items || items.length === 0) continue;
    items.sort((a, b) => a.name.localeCompare(b.name));
    sections.push(`${cat}:`);
    for (const t of items) {
      sections.push(`  ${t.name.padEnd(34)} ${t.title}`);
    }
    sections.push("");
  }
  const tools = sections.join("\n").trimEnd();

  return [
    `${SERVER_INFO.NAME} v${versionInfo().version} — CLI mode`,
    ``,
    `Usage:`,
    `  openl-mcp <url>                                  start the MCP server (stdio) for <url>`,
    `  openl-mcp <url> <tool-name> [args] [flags]       run one tool against <url>`,
    `  openl-mcp <tool-name> [args] [flags]             run one tool (url via --base-url / OPENL_BASE_URL)`,
    `  openl-mcp <tool-name> --help                     detailed help for a tool`,
    `  openl-mcp --list-tools                           JSON schemas of all tools`,
    `  openl-mcp --help                                 this message`,
    `  openl-mcp --version                              print version and build id (-V)`,
    ``,
    `Server URL (required — unless OPENL_BASE_URL is set):`,
    `  <url>    OpenL Studio base URL, e.g. http://localhost:8080. Pass it as the`,
    `           positional argument, or set the OPENL_BASE_URL environment variable`,
    `           (the positional takes precedence). With no arguments at all, the`,
    `           binary starts the MCP server on stdio using OPENL_BASE_URL — the`,
    `           default for Claude Desktop / Cursor / other MCP clients.`,
    ``,
    `Tool names: the \`openl_\` prefix is not used on the CLI — run \`list_repositories\`,`,
    `not \`openl_list_repositories\`. The prefix is added only on the MCP protocol wire;`,
    `the catalog below and \`--list-tools\` both report the bare names.`,
    ``,
    `Argument sources (mutually exclusive):`,
    `  '{"foo":"bar"}'    inline JSON literal`,
    `  @path/args.json    load JSON from file`,
    `  --stdin            read JSON from stdin`,
    ``,
    `Config flags (override the matching env vars):`,
    `  --base-url <url>            OPENL_BASE_URL`,
    `  --token <pat>               OPENL_PERSONAL_ACCESS_TOKEN`,
    `  --timeout <ms>              OPENL_TIMEOUT (default 30000)`,
    `  --cookie-jar <path>         persist JSESSIONID between calls (needed`,
    `                              for trace flow across separate npx runs)`,
    ``,
    `Authentication is optional: pass --token (or set OPENL_PERSONAL_ACCESS_TOKEN)`,
    `to authenticate; without it, requests are sent unauthenticated (for servers`,
    `that permit anonymous access).`,
    ``,
    `Tip: complex tools (update_table, append_table, …) take`,
    `large structured JSON — pass it via @file.json or --stdin rather than`,
    `inline. On Windows cmd, prefer @file.json (single quotes won't work).`,
    ``,
    `Output: defaults to structured JSON, the same as the MCP server.`,
    `For human-readable output, pass "response_format":"markdown" in args.`,
    ``,
    `Discovery:`,
    `  --help                      human-readable: this catalog of tool titles`,
    `  <tool> --help               human-readable: full description + arg schema`,
    `  --list-tools                machine-readable: JSON (bare name/title/description/schema)`,
    ``,
    `Examples:`,
    `  npx -y openl-mcp list_repositories`,
    `  echo '{"projectId":"p","comment":"fix CA rates"}' | \\`,
    `    npx -y openl-mcp save_project --stdin`,
    ``,
    `Available tools (${getAllTools().length}, grouped by category):`,
    tools,
    ``,
  ].join("\n");
}

/**
 * Lazily ensure the tool registry is populated. Safe to call repeatedly:
 * `registerAllTools` overwrites entries in its module-level map.
 */
let registered = false;
function ensureToolsRegistered(): void {
  if (registered) return;
  registerAllTools();
  registered = true;
}

/**
 * Entry point for CLI mode. Returns the process exit code.
 *
 * Pure with respect to its `options` (no direct `process.*` reads) so tests
 * can drive it with injected env/stdin/stdout/stderr.
 */
export async function runCli(options: RunCliOptions): Promise<number> {
  const stdout = options.stdout ?? process.stdout;
  const stderr = options.stderr ?? process.stderr;
  const stdin = options.stdin ?? process.stdin;
  const env = options.env ?? process.env;

  const parsed = parseArgs(options.argv);

  if (parsed.errors.length > 0) {
    for (const err of parsed.errors) stderr.write(`Error: ${err}\n`);
    stderr.write(`\nRun with --help for usage.\n`);
    return EXIT_CODES.USAGE;
  }

  // --version — single line, parseable, no config needed. The first two fields
  // stay `<name> <version>`, read from package.json, so this matches what
  // `npm view openl-mcp version` and the installed tarball report; the build id
  // and timestamp follow in a parenthetical, and identify WHICH build of that
  // version is installed (nightlies between two releases share the version).
  if (parsed.showVersion) {
    stdout.write(`${versionLine()}\n`);
    return EXIT_CODES.OK;
  }

  // Pure discovery commands — no config or network needed to list/describe tools.
  if (parsed.showHelp || parsed.listTools) {
    ensureToolsRegistered();
    if (parsed.showHelp) {
      // Tool-specific help: `<tool> --help` renders schema for that tool
      // instead of the global help. If the tool name is unknown, fall
      // through to a USAGE error.
      if (parsed.toolName) {
        // Accept the short (prefix-less) or fully-qualified name.
        const canonical = resolveToolName(parsed.toolName);
        const help = canonical ? renderToolHelp(canonical) : null;
        if (help === null) {
          stderr.write(`Error: Unknown tool: ${parsed.toolName}\n\nRun --list-tools to see available tools.\n`);
          return EXIT_CODES.USAGE;
        }
        stdout.write(help);
        return EXIT_CODES.OK;
      }
      stdout.write(renderHelp());
    } else {
      const summary = getAllTools().map((t) => ({
        name: t.name,
        title: t.title,
        description: t.description,
        inputSchema: t.inputSchema,
      }));
      stdout.write(`${JSON.stringify(summary, null, 2)}\n`);
    }
    return EXIT_CODES.OK;
  }

  // Tool-name validation is a USAGE concern and needs neither config nor the
  // network — run it *before* buildConfig so a typo'd tool returns EX_USAGE
  // even when OPENL_BASE_URL/auth are also missing (otherwise the config
  // error would mask the typo). The real client is wired in below for execution.
  ensureToolsRegistered();

  if (!parsed.toolName) {
    stderr.write(`Error: tool name is required\n\nRun with --help for usage.\n`);
    return EXIT_CODES.USAGE;
  }

  // Resolve the typed name (short or fully-qualified) to the canonical
  // registered name. Unknown tool is a user-typed-wrong-name case → EX_USAGE
  // (consistent with the `<tool> --help` path). Without this pre-check, the
  // error would surface from executeTool as an MCP error and misclassify as
  // GENERIC (1).
  const canonicalToolName = resolveToolName(parsed.toolName);
  if (!canonicalToolName) {
    stderr.write(
      `Error: Unknown tool: ${parsed.toolName}\n\nRun --list-tools to see available tools.\n`,
    );
    return EXIT_CODES.USAGE;
  }

  const restoreQuiet = setQuietMode();

  try {
    let config: Types.OpenLConfig;
    try {
      // Base URL precedence: positional <url> → --base-url flag → OPENL_BASE_URL.
      // buildConfig handles the flag-vs-env step; fold the positional in first.
      const effectiveOverrides = {
        ...parsed.overrides,
        baseUrl: parsed.baseUrlPositional ?? parsed.overrides.baseUrl,
      };
      config = buildConfig(env, effectiveOverrides);
    } catch (error) {
      // Config errors (missing OPENL_BASE_URL, no auth, bad URL) → EX_CONFIG
      throw new CliError(
        error instanceof Error ? error.message : String(error),
        EXIT_CODES.CONFIG,
      );
    }
    const client = (options.clientFactory ?? ((c) => new OpenLClient(c)))(config);

    // Cookie-jar: restore JSESSIONID from previous invocation so
    // session-coupled flows (trace) work across separate `npx` calls. The jar
    // is bound to (base URL, principal) so a session never leaks across
    // hosts/users.
    const cookieBinding = parsed.cookieJarPath ? computeCookieBinding(client) : null;
    if (parsed.cookieJarPath && cookieBinding) {
      const cookie = await loadCookieJar(parsed.cookieJarPath, cookieBinding, stderr);
      if (cookie !== null) client.setSessionCookie(cookie);
    }

    let toolArgs: unknown;
    try {
      toolArgs = await resolveToolArgs(parsed, stdin);
    } catch (error) {
      // Malformed JSON from inline / @file / --stdin → EX_DATAERR
      throw new CliError(
        error instanceof Error ? error.message : String(error),
        EXIT_CODES.DATAERR,
      );
    }

    const result = await executeTool(canonicalToolName, toolArgs, client);

    // Persist any session cookie established by this call for the next one.
    if (parsed.cookieJarPath && cookieBinding) {
      await saveCookieJar(parsed.cookieJarPath, client.getSessionCookie(), cookieBinding, stderr);
    }

    for (const part of result.content) {
      if (part.type === "text" && typeof part.text === "string") {
        stdout.write(part.text);
        if (!part.text.endsWith("\n")) stdout.write("\n");
      }
    }
    return EXIT_CODES.OK;
  } catch (error: unknown) {
    stderr.write(`Error: ${sanitizeError(error)}\n`);
    return classifyError(error);
  } finally {
    restoreQuiet();
  }
}

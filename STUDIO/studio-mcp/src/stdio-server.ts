/**
 * stdio transport for OpenL MCP Server
 *
 * Runs the MCP protocol over stdin/stdout — the transport MCP clients such as
 * Claude Desktop and Cursor launch, and the one a bare `openl-mcp <url>`
 * invocation starts. Single-session: one configured `Server` bound to the one
 * connected stdio transport.
 *
 * This module is not an entry point: the single binary entry, `index.ts`,
 * lazy-imports it and calls {@link startStdioServer} for the default
 * (non-`--http`, non-CLI) launch, so an HTTP or CLI launch never constructs the
 * stdio server.
 */
import { serveStdio } from "@modelcontextprotocol/server/stdio";
import { versionLine } from "./build-info.js";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { normalizeToken, sanitizeError } from "./utils.js";
import type { ParsedArgs } from "./cli.js";
import type * as Types from "./types.js";

/**
 * MCP Server for OpenL Studio over stdio.
 *
 * Handles MCP protocol communication and routes requests to the OpenL client.
 */
class OpenLMCPServer {
  private readonly config: Types.OpenLConfig;

  /**
   * Create a new MCP server instance
   *
   * @param config - OpenL Studio configuration
   */
  constructor(config: Types.OpenLConfig) {
    this.config = config;
  }

  /**
   * Start the MCP server
   */
  async start(): Promise<void> {
    // The v2 serving entry negotiates either the modern 2026-07-28 era or a
    // legacy 2025-era connection, then pins one fresh server/client pair for
    // the lifetime of that stdio connection.
    serveStdio(() => createConfiguredServer(new OpenLClient(this.config)), {
      onerror: (error) => console.error("MCP stdio transport error:", sanitizeError(error)),
    });
  }
}

/**
 * Explicit configuration overrides for the stdio server launch. Each field,
 * when defined, takes precedence over the matching `OPENL_*` environment
 * variable. Populated from the binary's command-line arguments (a positional
 * `<url>` and optional auth/timeout flags) in `startStdioServer`.
 */
interface ServerConfigOverrides {
  baseUrl?: string;
  personalAccessToken?: string;
  timeout?: number;
}

/**
 * Load configuration for the stdio transport (when an MCP client — or a direct
 * `openl-mcp <url>` invocation — launches the server).
 *
 * The base URL resolves from `overrides.baseUrl` (the positional `<url>` /
 * `--base-url`) first, then the `OPENL_BASE_URL` environment variable.
 * Authentication is OPTIONAL (OpenL Studio single-user mode accepts
 * unauthenticated requests); credentials, when present, come from overrides or
 * the environment.
 *
 * @param overrides - Command-line overrides; each falls back to its env var.
 * @returns OpenL Studio configuration
 * @throws Error if the base URL is missing or malformed, or timeout is invalid
 */
export async function loadConfigFromEnv(
  overrides: ServerConfigOverrides = {},
): Promise<Types.OpenLConfig> {
  console.error(`[Config] Resolving configuration (positional <url> / flags / environment)...`);
  console.error(`[Config] NOTE: This is for stdio transport. Auth credentials may come from MCP client config or CLI flags.`);
  const baseUrl = overrides.baseUrl ?? process.env.OPENL_BASE_URL;
  if (!baseUrl) {
    throw new Error(
      "OpenL base URL is required: pass it as a positional argument " +
        "(openl-mcp <url>) or set the OPENL_BASE_URL environment variable",
    );
  }

  // Validate base URL format
  try {
    new URL(baseUrl);
  } catch {
    throw new Error(`Invalid OpenL base URL: ${baseUrl}`);
  }

  // Parse and validate timeout — flag override first, then env.
  let timeout: number | undefined = overrides.timeout;
  if (timeout === undefined && process.env.OPENL_TIMEOUT) {
    const parsedTimeout = parseInt(process.env.OPENL_TIMEOUT, 10);
    if (isNaN(parsedTimeout) || parsedTimeout <= 0) {
      throw new Error(`Invalid OPENL_TIMEOUT value: ${process.env.OPENL_TIMEOUT}`);
    }
    timeout = parsedTimeout;
  }

  // An explicit token (flag/env) or nothing: a blank/whitespace token (e.g. an
  // unset `${user_config.studio_token}` that expands to "") is treated as
  // absent, so it never sends an empty credential; without a token requests
  // are anonymous (single-user Studio).
  const config: Types.OpenLConfig = {
    baseUrl,
    personalAccessToken: normalizeToken(overrides.personalAccessToken ?? process.env.OPENL_PERSONAL_ACCESS_TOKEN),
    timeout,
  };

  // Authentication is optional: OpenL Studio in single-user mode accepts
  // unauthenticated requests, so running without a token is normal and is not
  // reported. Confirm only when a token is present (its value stays hidden).
  if (config.personalAccessToken) {
    console.error(`[Config] Authentication:`);
    console.error(`[Config]   - Personal Access Token: configured (hidden)`);
  }

  return config;
}

/**
 * Launch the MCP server on the stdio transport.
 *
 * Resolves configuration from the parsed CLI arguments — the base URL comes
 * from the positional `<url>` first, then `--base-url`, then `OPENL_BASE_URL`;
 * auth/timeout may also be supplied as flags, each falling back to its env var
 * — then connects a single stdio session. Called by `index.ts` for the default
 * launch (no `--http`, not a CLI tool invocation).
 *
 * @param parsed - The parsed command-line arguments.
 */
export async function startStdioServer(parsed: ParsedArgs): Promise<void> {
  // --cookie-jar only applies to single CLI tool invocations; it has no effect
  // on the long-lived server. Warn rather than ignore silently, so a misplaced
  // flag doesn't look like it took effect.
  if (parsed.cookieJarPath !== undefined) {
    console.error("Warning: --cookie-jar is ignored when launching the MCP server (it applies only to single tool invocations).");
  }

  let config: Types.OpenLConfig;
  try {
    config = await loadConfigFromEnv({
      baseUrl: parsed.baseUrlPositional ?? parsed.overrides.baseUrl,
      personalAccessToken: parsed.overrides.token,
      timeout: parsed.overrides.timeout,
    });
  } catch (error: unknown) {
    // Missing/invalid base URL is a usage problem, not a crash — print a
    // clear, stack-trace-free message naming both ways to supply the URL,
    // then exit 1.
    console.error(`Error: ${sanitizeError(error)}`);
    console.error("");
    console.error("Usage:");
    console.error("  openl-mcp <url>                 start the MCP server for <url>");
    console.error("  OPENL_BASE_URL=<url> openl-mcp  start the MCP server using the env var");
    console.error("");
    console.error("Provide the OpenL Studio base URL as a positional argument, or via the");
    console.error("OPENL_BASE_URL environment variable. Run `openl-mcp --help` for full usage.");
    process.exit(1);
  }

  // Announce the exact build before serving. stderr is the MCP client's log
  // channel for a stdio server, so this line is what a user or support engineer
  // can read back from the client's logs to identify what actually ran.
  console.error(`[Server] ${versionLine()}`);

  const server = new OpenLMCPServer(config);
  await server.start();
}

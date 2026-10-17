#!/usr/bin/env node

/**
 * OpenL MCP Server — binary entry point
 *
 * Model Context Protocol server for OpenL Studio Rules Management System.
 *
 * This file is only the dispatcher: it inspects how the binary was invoked and
 * routes to one of three modes, each implemented in a sibling module that is
 * lazy-imported so a launch loads only the code it needs:
 * - stdio transport — `src/stdio-server.ts`
 * - Streamable HTTP transport — `src/http-server.ts`
 * - CLI / direct tool invocation — `src/cli.ts`
 *
 * @see https://github.com/openl-tablets/openl-mcp
 * @see https://modelcontextprotocol.io/
 */

import { realpathSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { sanitizeError } from "./utils.js";

/**
 * Wait until data already queued on `stream` has left the process.
 *
 * `process.exit()` abandons whatever is still queued. That is invisible when
 * output goes to a terminal or a file (those writes complete synchronously),
 * but a PIPE (`openl-mcp --list-tools | jq …`) is asynchronous: only the first
 * ~64 KB fits in the pipe buffer, so a larger payload used to be cut off
 * mid-JSON with a successful exit code.
 *
 * Queueing an empty chunk and awaiting its callback is enough to drain what IS
 * queued: a stream serves its queue in order, so this callback cannot run before
 * the earlier chunks have been handed to the OS. Backpressure from a slow reader
 * is therefore honoured rather than truncated — the same as any well-behaved CLI.
 *
 * A stream with an EMPTY queue is left strictly alone. Writing a sentinel chunk
 * into it would manufacture an `EPIPE` on a stream the command never used but
 * whose reader is already gone (`openl-mcp typo_tool | head -1` closes stdout
 * while the diagnostics go to stderr) — turning an untouched stream into a
 * fabricated I/O failure that replaces the command's real exit code. Same for a
 * stream already finished or destroyed: it can never drain.
 */
function flushStream(stream: NodeJS.WriteStream): Promise<void> {
  if (stream.writableLength === 0 || stream.writableEnded || stream.destroyed) {
    return Promise.resolve();
  }
  return new Promise((resolve) => {
    stream.write("", () => resolve());
  });
}

/**
 * Main entry point.
 *
 * Dispatches based on how the binary was invoked:
 * - `--http` flag → start the Express Streamable HTTP transport (Docker /
 *   `npm run start:http`). The base URL comes from the positional `<url>` /
 *   `--base-url`, falling back to `OPENL_BASE_URL`; the port comes from `PORT`;
 *   auth is per-session via the Authorization header. See `src/http-server.ts`.
 * - A tool name or discovery flag → CLI mode (direct API invocation via
 *   `executeTool`). See `src/cli.ts`.
 * - Otherwise (no args, or just a positional `<url>` / server flags) → start
 *   the MCP server on stdio (Claude Desktop / Cursor / other MCP clients).
 *   See `src/stdio-server.ts`.
 */
async function main(): Promise<void> {
  try {
    const cliArgs = process.argv.slice(2);

    const { parseArgs, isCliInvocation, runCli } = await import("./cli.js");

    // HTTP transport. `--http` is not a tool flag, so strip it before parsing,
    // then forward the resolved base URL (positional `<url>` / `--base-url`) so
    // `openl-mcp <url> --http` works — matching stdio and the documented
    // precedence (positional `<url>` > `--base-url` > `OPENL_BASE_URL`).
    // Lazy-import so a stdio/CLI launch never loads Express.
    if (cliArgs.includes("--http")) {
      const httpArgs = parseArgs(cliArgs.filter((arg) => arg !== "--http"));
      const { startHttpServer } = await import("./http-server.js");
      await startHttpServer({
        baseUrl: httpArgs.baseUrlPositional ?? httpArgs.overrides.baseUrl,
      });
      return;
    }

    const parsed = parseArgs(cliArgs);

    if (isCliInvocation(parsed)) {
      // CLI/tool mode: a tool name, a discovery flag (--help/--list-tools/
      // --version), a tool-argument source, or a parse error.
      //
      // The exit code the CLI settled on, once it is known. The stdout handler
      // below reports it instead of a bare 0: a reader that closed the pipe
      // early must not turn a failed command into a success.
      let cliExitCode: number | undefined;

      // EPIPE handling: when our stdout is piped into something that exits
      // early (`npx … | head -1`), the next write would throw EPIPE and crash
      // the process. Treat it as an early termination that keeps the command's
      // own verdict — 0 while the CLI has not reached one yet.
      // See https://github.com/nodejs/node-v0.x-archive/issues/3211
      process.stdout.on("error", (err: NodeJS.ErrnoException) => {
        if (err.code === "EPIPE") process.exit(cliExitCode ?? 0);
        throw err;
      });

      // stderr carries diagnostics, never results. Losing it (`2>&-`, a closed
      // pipe) must not crash the process on an unhandled 'error' — which exits
      // 1 and masks the real code — nor make a failure look successful. Swallow
      // every write failure here: the exit code below still tells the truth.
      process.stderr.on("error", () => { /* diagnostics channel gone */ });

      cliExitCode = await runCli({ argv: cliArgs });
      // Drain before exiting: `process.exit` would discard output still queued
      // in a pipe. Exiting explicitly (rather than letting the loop empty) keeps
      // the exit deterministic even if a tool leaves a handle open.
      await Promise.all([flushStream(process.stdout), flushStream(process.stderr)]);
      process.exit(cliExitCode);
    }

    // Default: launch the MCP server on the stdio transport.
    const { startStdioServer } = await import("./stdio-server.js");
    await startStdioServer(parsed);
  } catch (error: unknown) {
    const sanitizedMessage = sanitizeError(error);
    console.error("Failed to start OpenL MCP server:", sanitizedMessage);
    process.exit(1);
  }
}

/**
 * True when this module is the process entry point (run directly), false when
 * it's merely imported (e.g. by the test suite, which must not start a server).
 *
 * Compares the realpath of `process.argv[1]` to this module's own path. Using
 * realpaths is essential: when the binary is launched through a `bin` symlink
 * — which is how a global install (`npm i -g`) and npm's `.bin/` shims invoke
 * it — `process.argv[1]` is the UNRESOLVED symlink path (e.g. `.../bin/openl-
 * mcp-server`), so the previous `=== file://argv[1]` / `endsWith('index.js')`
 * check missed it and `main()` never ran. realpath resolves the symlink to the
 * real `dist/index.js`, and also smooths over platform path quirks (e.g. macOS
 * `/tmp` → `/private/tmp`). Falls back to `false` if the path can't be resolved.
 */
function isMainEntryPoint(): boolean {
  if (!process.argv[1]) return false;
  try {
    return realpathSync(process.argv[1]) === fileURLToPath(import.meta.url);
  } catch {
    return false;
  }
}

if (isMainEntryPoint()) {
  main();
}

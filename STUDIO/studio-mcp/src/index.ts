/**
 * OpenL MCP Server — entry point.
 *
 * Starts the Streamable HTTP transport of `src/http-server.ts`. OpenL Studio runs
 * this file with `PORT`, `HOST` and `OPENL_VERSION` set, see `McpServerProcess`
 * in `studio-backend`, and names its own address with every request, in the
 * `OpenL-Base-Url` header.
 */
import { startHttpServer } from "./http-server.js";
import { sanitizeError } from "./utils.js";

try {
  await startHttpServer();
} catch (error: unknown) {
  console.error("Failed to start OpenL MCP server:", sanitizeError(error));
  process.exit(1);
}

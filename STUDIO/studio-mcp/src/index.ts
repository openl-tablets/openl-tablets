/**
 * OpenL MCP Server — entry point.
 *
 * Starts the Streamable HTTP transport of `src/http-server.ts`. OpenL Studio runs
 * this file with `PORT`, `HOST` and `OPENL_VERSION` set, see `McpServerProcess`
 * in `studio-backend`, and names its own address with every request, in the
 * `OpenL-Base-Url` header.
 *
 * It sends OpenTelemetry data only when `OTEL_SDK_DISABLED` is `false`, which
 * Studio sets when it sends telemetry itself, see `telemetry.ts`.
 */
import { startHttpServer } from "./http-server.js";
import { sanitizeError } from "./utils.js";

if (process.env.OTEL_SDK_DISABLED === "false") {
  try {
    const { startTelemetry } = await import("./telemetry.js");
    const stopTelemetry = startTelemetry();
    // Studio stops the server with SIGTERM: the telemetry still buffered is sent before it exits
    process.once("SIGTERM", () => void stopTelemetry().finally(() => process.exit(0)));
  } catch (error: unknown) {
    // The server runs without telemetry rather than not at all
    console.error("Failed to start OpenTelemetry:", sanitizeError(error));
  }
}

try {
  await startHttpServer();
} catch (error: unknown) {
  console.error("Failed to start OpenL MCP server:", sanitizeError(error));
  process.exit(1);
}

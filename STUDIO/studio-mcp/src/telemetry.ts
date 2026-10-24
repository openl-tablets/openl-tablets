/**
 * The OpenTelemetry SDK of the server.
 *
 * The bundle carries it as a chunk of its own, which `index.ts` loads only when OpenL Studio sends telemetry and
 * says so with `OTEL_SDK_DISABLED=false`. The SDK reads the rest of its settings from the `OTEL_*` variables Studio
 * passes on: the exporters, the endpoint and the headers of the collector, the name of the service.
 *
 * The server traces the requests it serves and the calls it makes to Studio, which carry the trace on, and measures
 * their durations.
 */
import { createRequire } from "node:module";
import { HttpInstrumentation } from "@opentelemetry/instrumentation-http";
import { UndiciInstrumentation } from "@opentelemetry/instrumentation-undici";
import { NodeSDK } from "@opentelemetry/sdk-node";

/** Starts the SDK and returns what flushes and stops it. */
export function startTelemetry(): () => Promise<void> {
  const sdk = new NodeSDK({ instrumentations: [new HttpInstrumentation(), new UndiciInstrumentation()] });
  sdk.start();
  // The server imports node:http as an ES module, which no hook of the instrumentation sees. A require of it lets
  // the instrumentation patch the server class, which the module and the require share.
  createRequire(import.meta.url)("node:http");
  return () => sdk.shutdown();
}

/**
 * The span and the duration metric of a tool call, after the OpenTelemetry semantic conventions of MCP.
 *
 * They go through the OpenTelemetry API, which does nothing until the SDK of `telemetry.ts` is started: a server
 * that sends no telemetry pays for neither.
 */
import { metrics, SpanStatusCode, trace, type Histogram } from "@opentelemetry/api";
import { ProtocolError } from "@modelcontextprotocol/server";

import { SERVER_INFO } from "./constants.js";

/** The bounds of the buckets of the duration, in seconds, the semantic conventions advise. */
const DURATION_BUCKETS = [0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 30, 60, 120, 300];

/** A tracer of the API hands its spans to the SDK once the SDK is started, so one serves every call. */
const tracer = trace.getTracer(SERVER_INFO.NAME, SERVER_INFO.VERSION);

let duration: Histogram | undefined;

/** The histogram of the durations, made at the first call: the SDK is started by then, so it records. */
function durationHistogram(): Histogram {
  duration ??= metrics.getMeter(SERVER_INFO.NAME, SERVER_INFO.VERSION).createHistogram(
    "mcp.server.operation.duration",
    {
      description: "The duration of an MCP request as the server handles it.",
      unit: "s",
      advice: { explicitBucketBoundaries: DURATION_BUCKETS },
    },
  );
  return duration;
}

/** The `error.type` of a failed call: the JSON-RPC code of a protocol error, else the name of the error. */
function errorType(error: unknown): string {
  if (error instanceof ProtocolError) return String(error.code);
  return error instanceof Error ? error.name : "Error";
}

/** Runs a call of the tool `tool`, its name on the wire, in a span of its own and records how long it took. */
export function traceToolCall<T>(tool: string, call: () => Promise<T>): Promise<T> {
  const attributes: Record<string, string> = {
    "mcp.method.name": "tools/call",
    "gen_ai.operation.name": "execute_tool",
    "gen_ai.tool.name": tool,
  };
  const start = performance.now();
  return tracer.startActiveSpan(`tools/call ${tool}`, { attributes }, async (span) => {
    try {
      return await call();
    } catch (error: unknown) {
      const type = errorType(error);
      attributes["error.type"] = type;
      span.setAttribute("error.type", type);
      span.setStatus({ code: SpanStatusCode.ERROR });
      throw error;
    } finally {
      span.end();
      durationHistogram().record((performance.now() - start) / 1000, attributes);
    }
  });
}

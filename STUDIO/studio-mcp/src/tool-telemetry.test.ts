import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import { afterAll, beforeAll, describe, expect, it } from "vitest";

import { startTelemetry } from "./telemetry.js";
import { attributeOf, startOtlpCollector, telemetryEnvironment } from "./testing/otlp-collector.js";
import { traceToolCall } from "./tool-telemetry.js";

let collector: Awaited<ReturnType<typeof startOtlpCollector>>;

beforeAll(async () => {
  collector = await startOtlpCollector();
});

afterAll(() => {
  collector.server.close();
});

describe("traceToolCall", () => {
  it("traces and measures every tool call, naming the error of a failed one", async () => {
    telemetryEnvironment(collector.server, "OpenL-mcp");
    const stop = startTelemetry();

    await expect(traceToolCall("openl_list_repositories", () => Promise.resolve("[]"))).resolves.toBe("[]");
    await expect(traceToolCall("openl_get_table", () =>
      Promise.reject(new ProtocolError(ProtocolErrorCode.InvalidParams, "no table")))).rejects.toThrow("no table");
    await stop();

    const spans = collector.spans();
    const listed = spans.find((s) => s.name === "tools/call openl_list_repositories");
    expect(attributeOf(listed, "gen_ai.tool.name")).toBe("openl_list_repositories");
    expect(attributeOf(listed, "error.type")).toBeUndefined();
    const failed = spans.find((s) => s.name === "tools/call openl_get_table");
    expect(attributeOf(failed, "error.type")).toBe(String(ProtocolErrorCode.InvalidParams));
    expect(collector.metricNames()).toContain("mcp.server.operation.duration");
  });
});

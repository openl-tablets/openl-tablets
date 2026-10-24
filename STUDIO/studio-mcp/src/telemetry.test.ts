import { once } from "node:events";
import { createServer, type Server } from "node:http";
import { afterAll, beforeAll, describe, expect, it } from "vitest";

import { startTelemetry } from "./telemetry.js";
import { addressOf, startOtlpCollector, telemetryEnvironment } from "./testing/otlp-collector.js";

let collector: Awaited<ReturnType<typeof startOtlpCollector>>;
let studio: Server;

beforeAll(async () => {
  collector = await startOtlpCollector();
  studio = createServer((_req, res) => res.writeHead(200).end("[]")).listen(0, "127.0.0.1");
  await once(studio, "listening");
});

afterAll(() => {
  collector.server.close();
  studio.close();
});

describe("startTelemetry", () => {
  it("sends the spans and the metrics of the requests to the collector of the environment", async () => {
    telemetryEnvironment(collector.server, "OpenL-mcp");
    const stop = startTelemetry();

    await fetch(addressOf(studio)).then((r) => r.text());
    await stop();

    expect(collector.resource()).toContainEqual({ key: "service.name", value: { stringValue: "OpenL-mcp" } });
    // The call to Studio carries the trace on, so the request Studio served belongs to it
    const spans = collector.spans();
    expect(spans.map((s) => s.name)).toEqual(["GET", "GET"]);
    expect(new Set(spans.map((s) => s.traceId)).size).toBe(1);
    expect(collector.metricNames()).toEqual(
      expect.arrayContaining(["http.client.request.duration", "http.server.request.duration"]));
  });
});

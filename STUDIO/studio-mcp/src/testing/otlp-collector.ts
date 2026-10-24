/**
 * A collector of OTLP/HTTP in JSON for the tests of the telemetry: it keeps what it receives, by the path it was sent
 * to, and answers every post with success.
 */
import { once } from "node:events";
import { createServer, type Server } from "node:http";
import type { AddressInfo } from "node:net";

interface OtlpAttribute {
  key: string;
  value: { stringValue?: string };
}

export interface OtlpSpan {
  name: string;
  traceId: string;
  attributes: OtlpAttribute[];
}

interface OtlpTraces {
  resourceSpans: Array<{ resource: { attributes: OtlpAttribute[] }; scopeSpans: Array<{ spans: OtlpSpan[] }> }>;
}

interface OtlpMetrics {
  resourceMetrics: Array<{ scopeMetrics: Array<{ metrics: Array<{ name: string }> }> }>;
}

/** The address of a server listening on the loopback interface. */
export function addressOf(server: Server): string {
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

/** A collector listening on a free port of the loopback interface. */
export async function startOtlpCollector() {
  const received = new Map<string, unknown[]>();
  const server = createServer(async (req, res) => {
    const chunks: Buffer[] = [];
    for await (const chunk of req as AsyncIterable<Buffer>) chunks.push(chunk);
    const path = req.url ?? "";
    received.set(path, [...(received.get(path) ?? []), JSON.parse(Buffer.concat(chunks).toString())]);
    res.writeHead(200, { "Content-Type": "application/json" }).end("{}");
  }).listen(0, "127.0.0.1");
  await once(server, "listening");
  const traces = () => (received.get("/v1/traces") ?? []) as OtlpTraces[];
  return {
    server,
    /** The resource attributes of the first batch of spans. */
    resource: () => traces()[0]?.resourceSpans[0]?.resource.attributes ?? [],
    /** Every span received. */
    spans: () => traces()
      .flatMap((t) => t.resourceSpans)
      .flatMap((r) => r.scopeSpans)
      .flatMap((s) => s.spans),
    /** The names of every metric received. */
    metricNames: () => ((received.get("/v1/metrics") ?? []) as OtlpMetrics[])
      .flatMap((m) => m.resourceMetrics)
      .flatMap((r) => r.scopeMetrics)
      .flatMap((s) => s.metrics)
      .map((metric) => metric.name),
  };
}

/** Sets the environment the SDK of the server reads: OTLP/HTTP JSON to the collector, as the named service. */
export function telemetryEnvironment(collector: Server, serviceName: string): void {
  Object.assign(process.env, {
    OTEL_EXPORTER_OTLP_ENDPOINT: addressOf(collector),
    OTEL_EXPORTER_OTLP_PROTOCOL: "http/json",
    OTEL_LOGS_EXPORTER: "none",
    OTEL_SERVICE_NAME: serviceName,
  });
}

/** The string value of an attribute of a span. */
export function attributeOf(span: OtlpSpan | undefined, key: string): string | undefined {
  return span?.attributes.find((a) => a.key === key)?.value.stringValue;
}

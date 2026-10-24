import { metrics, propagation, SpanKind, trace, type Attributes, type Meter } from '@opentelemetry/api'
import { FetchInstrumentation } from '@opentelemetry/browser-instrumentation/experimental/fetch'
import { hrTimeToMilliseconds, W3CTraceContextPropagator } from '@opentelemetry/core'
import { AggregationTemporalityPreference, OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-http'
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http'
import { resourceFromAttributes } from '@opentelemetry/resources'
import { MeterProvider, PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics'
import { BatchSpanProcessor, TracerProvider, type ReadableSpan, type SpanProcessor } from '@opentelemetry/sdk-trace'
import {
    ATTR_ERROR_TYPE,
    ATTR_HTTP_REQUEST_METHOD,
    ATTR_HTTP_RESPONSE_STATUS_CODE,
    ATTR_SERVER_ADDRESS,
    ATTR_SERVER_PORT,
    ATTR_SERVICE_NAME,
    METRIC_HTTP_CLIENT_REQUEST_DURATION,
} from '@opentelemetry/semantic-conventions'
import { TELEMETRY_RELAY, type TelemetrySettings } from './telemetry'
import { escapeRegExp } from '../utils/escapeRegExp'

/** The bounds of the buckets of a request duration, in seconds, the semantic conventions of HTTP advise. */
const DURATION_BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10]

/** The attributes of a request span the duration metric keeps: the ones of low cardinality. */
const DURATION_ATTRIBUTES = [
    ATTR_HTTP_REQUEST_METHOD,
    ATTR_HTTP_RESPONSE_STATUS_CODE,
    ATTR_SERVER_ADDRESS,
    ATTR_SERVER_PORT,
    ATTR_ERROR_TYPE,
]

const REDACTED = 'REDACTED'

/** The last part of a path that is a secret: the token of an e-mail verification. */
const SECRET_PATH = /(\/mail\/verify\/)[^/]+/

/**
 * The address of a request as a span records it: without the credentials of the address, the values of its query,
 * which may name what a user searched for or carry a token, the token of an e-mail verification, and the fragment,
 * which never reaches the server.
 */
export const redactedUrl = (url: string): string => {
    const address = new URL(url, location.href)
    address.username = ''
    address.password = ''
    for (const name of new Set(address.searchParams.keys())) {
        address.searchParams.set(name, REDACTED)
    }
    address.pathname = address.pathname.replace(SECRET_PATH, `$1${REDACTED}`)
    address.hash = ''
    return address.href
}

/** Records `http.client.request.duration` of every request span that ends. */
const requestDurations = (meter: Meter): SpanProcessor => {
    const histogram = meter.createHistogram(METRIC_HTTP_CLIENT_REQUEST_DURATION, {
        description: 'Duration of HTTP client requests.',
        unit: 's',
        advice: { explicitBucketBoundaries: DURATION_BUCKETS },
    })
    return {
        onStart: () => undefined,
        onEnd: (span: ReadableSpan) => {
            if (span.kind === SpanKind.CLIENT) {
                const attributes: Attributes = Object.fromEntries(
                    DURATION_ATTRIBUTES.filter(key => span.attributes[key] !== undefined)
                        .map(key => [key, span.attributes[key]])
                )
                histogram.record(hrTimeToMilliseconds(span.duration) / 1000, attributes)
            }
        },
        forceFlush: () => Promise.resolve(),
        shutdown: () => Promise.resolve(),
    }
}

/**
 * Starts the OpenTelemetry SDK of the page: a span for every `fetch` of the page and the duration metric of the
 * requests, posted in OTLP/HTTP to the server, which passes them on to the collector.
 *
 * With traces on, a request to the server carries the trace on in its `traceparent` header, so the spans of the
 * server join the one of the page. With metrics only, the spans feed the metric and are not sent. The posts to the
 * server are not traced themselves.
 *
 * The metrics are deltas: each tab sends what it measured since its last export, so the tabs of many users add up in
 * one series, and an idle tab sends nothing, which would otherwise keep its session alive.
 *
 * A span records the address of a request without its secrets, see {@link redactedUrl}.
 *
 * The page flushes what it has buffered when it is hidden, since it may never come back.
 */
export const startOpenTelemetry = (settings: TelemetrySettings): void => {
    const resource = resourceFromAttributes({ ...settings.attributes, [ATTR_SERVICE_NAME]: settings.serviceName })
    const spanProcessors: SpanProcessor[] = []
    if (settings.metrics) {
        const meterProvider = new MeterProvider({
            resource,
            readers: [new PeriodicExportingMetricReader({
                exporter: new OTLPMetricExporter({
                    url: `${TELEMETRY_RELAY}/v1/metrics`,
                    temporalityPreference: AggregationTemporalityPreference.DELTA,
                }),
            })],
        })
        metrics.setGlobalMeterProvider(meterProvider)
        spanProcessors.push(requestDurations(meterProvider.getMeter('studio-ui')))
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'hidden') {
                void meterProvider.forceFlush()
            }
        })
    }
    if (settings.traces) {
        spanProcessors.push(new BatchSpanProcessor({
            exporter: new OTLPTraceExporter({ url: `${TELEMETRY_RELAY}/v1/traces` }),
        }))
        propagation.setGlobalPropagator(new W3CTraceContextPropagator())
    }
    const tracerProvider = new TracerProvider({ resource, spanProcessors })
    trace.setGlobalTracerProvider(tracerProvider)
    const relay = new RegExp(escapeRegExp(`${TELEMETRY_RELAY}/v1/`))
    new FetchInstrumentation({ ignoreUrls: [relay], sanitizeUrl: redactedUrl })
        .setTracerProvider(tracerProvider)
}

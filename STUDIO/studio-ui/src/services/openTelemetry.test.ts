import { metrics, propagation, trace, type Attributes, type ProxyTracerProvider } from '@opentelemetry/api'
import { AggregationTemporality, type MeterProvider, type ResourceMetrics } from '@opentelemetry/sdk-metrics'
import type { ReadableSpan, TracerProvider } from '@opentelemetry/sdk-trace'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { redactedUrl, startOpenTelemetry } from './openTelemetry'

/** What the OTLP exporters were made for and handed: they stand for the posts to the server. */
const exported = vi.hoisted(() => ({
    urls: [] as string[],
    spans: [] as ReadableSpan[],
    metrics: [] as ResourceMetrics[],
}))

/**
 * An OTLP exporter that keeps what it is handed, by the `sink` it is made for. A metric exporter takes the temporality
 * it is asked for, as the real one does: the preferences share the values of the temporalities.
 */
const fakeExporter = vi.hoisted(() => <T>(sink: (item: T) => void) => class {
    readonly temporality: number

    constructor(config: { url: string, temporalityPreference?: number }) {
        exported.urls.push(config.url)
        this.temporality = config.temporalityPreference ?? 1
    }

    selectAggregationTemporality() {
        return this.temporality
    }

    export(item: T, done: (result: { code: number }) => void) {
        sink(item)
        done({ code: 0 })
    }

    forceFlush() {
        return Promise.resolve()
    }

    shutdown() {
        return Promise.resolve()
    }
})

vi.mock('@opentelemetry/exporter-trace-otlp-http', () => ({
    OTLPTraceExporter: fakeExporter((spans: ReadableSpan[]) => exported.spans.push(...spans)),
}))

vi.mock('@opentelemetry/exporter-metrics-otlp-http', async importOriginal => ({
    ...await importOriginal<object>(),
    OTLPMetricExporter: fakeExporter((collected: ResourceMetrics) => exported.metrics.push(collected)),
}))

/** The points of the duration metric in the last export. */
const durationPoints = () => (exported.metrics.at(-1)?.scopeMetrics ?? [])
    .flatMap(s => s.metrics)
    .filter(m => m.descriptor.name === 'http.client.request.duration')
    .flatMap(m => m.dataPoints as ReadonlyArray<{ attributes: Attributes }>)

/** The one item of a list. */
const only = <T, >(items: readonly T[]): T => {
    expect(items).toHaveLength(1)
    return items[0] as T
}

/**
 * Flushes the SDK until it sent the request and the given number of spans: the span of a request ends once the page
 * has read the body of the response.
 */
const flushed = (spans: number) => vi.waitFor(async () => {
    await (metrics.getMeterProvider() as MeterProvider).forceFlush()
    await ((trace.getTracerProvider() as ProxyTracerProvider).getDelegate() as TracerProvider).forceFlush()
    expect(durationPoints()).toHaveLength(1)
    expect(exported.spans).toHaveLength(spans)
})

describe('startOpenTelemetry', () => {
    const server = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>()
    const settings = { serviceName: 'OpenL-ui', attributes: { 'deployment.environment': 'test' } }

    /** The headers of the request of the page to `/rest/projects`. */
    const sentHeaders = () =>
        new Headers(only(server.mock.calls.filter(([input]) => String(input).includes('/rest/projects')))[1]?.headers)

    beforeEach(() => {
        exported.urls.length = 0
        exported.spans.length = 0
        exported.metrics.length = 0
        server.mockImplementation(() => Promise.resolve(new Response('[]', { status: 200 })))
        vi.stubGlobal('fetch', server)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
        trace.disable()
        metrics.disable()
        propagation.disable()
    })

    it('traces the requests of the page and carries the trace on to the server', async () => {
        startOpenTelemetry({ ...settings, traces: true, metrics: true })

        await fetch('/rest/projects?search=Premium')
        await fetch('/rest/telemetry/v1/traces', { method: 'POST' })
        await flushed(1)

        expect(exported.urls).toEqual(['/rest/telemetry/v1/metrics', '/rest/telemetry/v1/traces'])
        const span = only(exported.spans)
        expect(span.attributes['url.full']).toBe('http://localhost:3000/rest/projects?search=REDACTED')
        expect(span.resource.attributes).toMatchObject({
            'service.name': 'OpenL-ui',
            'deployment.environment': 'test',
        })
        expect(sentHeaders().get('traceparent')).toContain(span.spanContext().traceId)
        expect(only(durationPoints()).attributes).toEqual({
            'http.request.method': 'GET',
            'http.response.status_code': 200,
            'server.address': 'localhost',
            'server.port': 3000,
        })
    })

    it('measures the requests without sending their spans or carrying the trace on', async () => {
        startOpenTelemetry({ ...settings, traces: false, metrics: true })

        await fetch('/rest/projects')
        await flushed(0)

        expect(exported.urls).toEqual(['/rest/telemetry/v1/metrics'])
        expect(sentHeaders().has('traceparent')).toBe(false)
        expect(exported.metrics.at(-1)?.scopeMetrics[0]?.metrics[0]?.aggregationTemporality)
            .toBe(AggregationTemporality.DELTA)
    })

    it('sends no metrics while the page measures nothing, so an idle tab does not keep its session alive', async () => {
        startOpenTelemetry({ ...settings, traces: false, metrics: true })
        await fetch('/rest/projects')
        await flushed(0)
        const sent = exported.metrics.length

        await (metrics.getMeterProvider() as MeterProvider).forceFlush()
        await (metrics.getMeterProvider() as MeterProvider).forceFlush()

        expect(exported.metrics).toHaveLength(sent)
    })

    it('sends the metrics it holds when the page is hidden', async () => {
        startOpenTelemetry({ ...settings, traces: false, metrics: true })
        metrics.getMeter('test').createCounter('opened').add(1)
        const hidden = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
        try {
            document.dispatchEvent(new Event('visibilitychange'))

            // The pages of the earlier tests flush their own providers too: the counter tells this one apart
            await vi.waitFor(() => expect(exported.metrics.flatMap(m => m.scopeMetrics.flatMap(s => s.metrics))
                .map(m => m.descriptor.name)).toContain('opened'))
        } finally {
            hidden.mockRestore()
        }
    })
})

describe('redactedUrl', () => {
    it('leaves out the credentials, the values of the query, the token of an e-mail verification, the fragment', () => {
        expect(redactedUrl('https://jane:secret@studio.example/rest/projects?search=Premium&fields=id,name#token=x'))
            .toBe('https://studio.example/rest/projects?search=REDACTED&fields=REDACTED')
        expect(redactedUrl('/rest/mail/verify/a1b2c3')).toBe('http://localhost:3000/rest/mail/verify/REDACTED')
        expect(redactedUrl('/rest/projects/ABC')).toBe('http://localhost:3000/rest/projects/ABC')
    })
})

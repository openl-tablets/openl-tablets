import apiCall, { LOCAL_LOAD_API_OPTIONS } from './apiCall'
import CONFIG from './config'
import { errorHandler } from '../utils/errorHandling'

/** What the pages send to OpenTelemetry, as `GET /rest/telemetry` tells it, and the service they send it as. */
export interface TelemetrySettings {
    serviceName: string
    /** The resource attributes of `OTEL_RESOURCE_ATTRIBUTES`, but the name of the service. */
    attributes: Record<string, string>
    traces: boolean
    metrics: boolean
}

/** Where the pages post their OTLP data, which the server passes on to the collector. */
export const TELEMETRY_RELAY = `${CONFIG.API_ROOT}/telemetry`

let started: Promise<void> | undefined

/**
 * Starts the OpenTelemetry of the page once the user is signed in, when the server sends telemetry.
 *
 * The SDK is a chunk of its own, loaded only then, so a server that sends no telemetry costs the page one small
 * request. The page starts it once; a failure leaves the page without telemetry and is only logged.
 */
export const startTelemetry = (): Promise<void> => {
    started ??= (async () => {
        try {
            const settings = await apiCall('/telemetry', undefined, LOCAL_LOAD_API_OPTIONS) as TelemetrySettings
            if (settings.traces || settings.metrics) {
                const { startOpenTelemetry } = await import('./openTelemetry')
                startOpenTelemetry(settings)
            }
        } catch (error) {
            errorHandler.logError(error instanceof Error ? error : new Error(String(error)), {
                message: 'OpenTelemetry is not started',
            })
        }
    })()
    return started
}

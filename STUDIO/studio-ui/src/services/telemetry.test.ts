import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiCall = vi.hoisted(() => vi.fn())
const startOpenTelemetry = vi.hoisted(() => vi.fn())
const logError = vi.hoisted(() => vi.fn())

vi.mock('./apiCall', () => ({ default: apiCall, LOCAL_LOAD_API_OPTIONS: { throwError: true } }))
vi.mock('./openTelemetry', () => ({ startOpenTelemetry }))
vi.mock('../utils/errorHandling', () => ({ errorHandler: { logError } }))

/** A fresh module, since the page starts the telemetry only once. */
const load = async () => {
    vi.resetModules()
    return (await import('./telemetry')).startTelemetry
}

describe('startTelemetry', () => {
    const settings = { serviceName: 'OpenL-ui', attributes: {}, traces: true, metrics: false }

    beforeEach(() => {
        apiCall.mockReset()
    })

    it('starts the SDK with the settings of the server, once', async () => {
        apiCall.mockResolvedValue(settings)
        const startTelemetry = await load()

        await startTelemetry()
        await startTelemetry()

        expect(apiCall).toHaveBeenCalledExactlyOnceWith('/telemetry', undefined, { throwError: true })
        expect(startOpenTelemetry).toHaveBeenCalledExactlyOnceWith(settings)
    })

    it('leaves the SDK unloaded when the server sends no telemetry', async () => {
        apiCall.mockResolvedValue({ ...settings, traces: false })
        const startTelemetry = await load()

        await startTelemetry()

        expect(startOpenTelemetry).not.toHaveBeenCalled()
    })

    it('runs the page without telemetry when the settings cannot be read', async () => {
        apiCall.mockRejectedValue(new Error('Forbidden'))
        const startTelemetry = await load()

        await startTelemetry()

        expect(startOpenTelemetry).not.toHaveBeenCalled()
        expect(logError).toHaveBeenCalledExactlyOnceWith(new Error('Forbidden'),
            { message: 'OpenTelemetry is not started' })
    })
})

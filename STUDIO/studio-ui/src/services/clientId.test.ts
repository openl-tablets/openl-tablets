import { afterEach, describe, expect, it, vi } from 'vitest'
import { CLIENT_ID } from './clientId'

/** What the backend's `ChangeOriginResolver` accepts; anything else is ignored there. */
const OPAQUE_TOKEN = /^[A-Za-z0-9_.-]{1,64}$/

describe('CLIENT_ID', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('is a token the backend accepts on a change request', () => {
        expect(CLIENT_ID).toMatch(OPAQUE_TOKEN)
    })

    it('is drawn from the random bytes of a plain-http install, where randomUUID is unavailable', async () => {
        // `randomUUID` needs a secure context; `getRandomValues` does not, and an id nobody can
        // guess is what keeps another client from passing its changes off as this tab's own echo.
        vi.stubGlobal('crypto', { getRandomValues: (bytes: Uint8Array) => bytes.fill(7) })
        vi.resetModules()

        const { CLIENT_ID: fromBytes } = await import('./clientId')

        expect(fromBytes).toMatch(OPAQUE_TOKEN)
        expect(fromBytes).toBe('07'.repeat(16))
    })

    it('still names the tab where the browser offers no cryptographic source at all', async () => {
        vi.stubGlobal('crypto', {})
        vi.resetModules()

        const { CLIENT_ID: fallback } = await import('./clientId')

        expect(fallback).toMatch(OPAQUE_TOKEN)
    })
})

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

    it('is built without crypto where the browser has none — a plain-http install', async () => {
        vi.stubGlobal('crypto', {})
        vi.resetModules()

        const { CLIENT_ID: fallback } = await import('./clientId')

        expect(fallback).toMatch(OPAQUE_TOKEN)
    })
})

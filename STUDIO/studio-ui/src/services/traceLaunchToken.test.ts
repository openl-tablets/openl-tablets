import { currentTraceLaunch, retireTraceLaunch, stampTraceLaunch } from './traceLaunchToken'

describe('traceLaunchToken', () => {
    // jsdom ships no Web Storage, so the token gets an in-memory one for this file.
    const memory = new Map<string, string>()

    beforeEach(() => {
        memory.clear()
        vi.stubGlobal('localStorage', {
            getItem: (key: string) => memory.get(key) ?? null,
            setItem: (key: string, value: string) => memory.set(key, value),
            removeItem: (key: string) => memory.delete(key),
            clear: () => memory.clear(),
        })
    })

    it('stamps a fresh token on every launch', () => {
        expect(currentTraceLaunch()).toBeNull()

        const first = stampTraceLaunch()
        const second = stampTraceLaunch()

        expect(first).not.toBe(second)
        expect(currentTraceLaunch()).toBe(second)
    })

    it('restores the previous token when a reserved launch is retired', () => {
        const previous = stampTraceLaunch()
        const reserved = stampTraceLaunch()

        retireTraceLaunch(reserved)

        expect(currentTraceLaunch()).toBe(previous)
    })

    it('keeps a newer launch token when retiring a stale reservation', () => {
        const reserved = stampTraceLaunch()
        const newer = stampTraceLaunch()

        retireTraceLaunch(reserved)

        expect(currentTraceLaunch()).toBe(newer)
    })
})

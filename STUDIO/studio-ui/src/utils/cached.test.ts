import { describe, expect, it, vi } from 'vitest'
import { cached } from './cached'

describe('cached', () => {
    it('creates the value of a key once and hands out the same one after that', () => {
        const cache = new Map<string, object>()
        const create = vi.fn(() => ({}))

        const first = cached(cache, 'a', create)

        expect(cached(cache, 'a', create)).toBe(first)
        expect(cached(cache, 'b', create)).not.toBe(first)
        expect(create).toHaveBeenCalledTimes(2)
    })
})

import { restoreNativeGlobals } from 'utils/prototypeJsCompat'

// Covers the "clean boot, legacy scripts injected later" order. The opposite order — Prototype.js already
// loaded when the bundle boots — is covered by the polluted-layout test in tableGraphLayout.test.ts.
describe('restoreNativeGlobals', () => {
    // Runs first on purpose: the shim locks the globals it repairs, so the polluted state has to be staged before
    // any other case has called it.
    it('restores Array.from after Prototype.js has replaced it with $A', () => {
        // Prototype.js does `Array.from = $A`, and $A ignores the map callback: it copies the source and returns
        // it. Array.from({ length: n }, fn) then yields n holes, JSON.stringify writes them as null, and a
        // generated table went to the server as "source":[[null],[null]] (EPBDS-16313).
        const $A = (iterable: { length?: number }) => {
            const length = iterable?.length ?? 0
            const results = Array(length) as unknown[]
            for (let i = length - 1; i >= 0; i--) {
                results[i] = (iterable as Record<number, unknown>)[i]
            }
            return results
        }
        Object.defineProperty(Array, 'from', { value: $A, writable: true, configurable: true })
        expect(Array.from({ length: 2 }, () => 'cell')).toEqual([undefined, undefined])

        restoreNativeGlobals()

        expect(Array.from({ length: 2 }, () => 'cell')).toEqual(['cell', 'cell'])
        expect(Array.from({ length: 2 }, (_, index) => index)).toEqual([0, 1])
        expect(Array.from('ab')).toEqual(['a', 'b'])
        expect(Array.from(new Set([1, 2]), value => value * 2)).toEqual([2, 4])
        // Prototype.js assigns in sloppy mode, which no-ops on the locked property
        expect(Reflect.set(Array, 'from', $A)).toBe(false)
    })

    it('keeps the healthy implementation, locks it against later Prototype.js pollution', () => {
        const native = Object.values
        restoreNativeGlobals()

        expect(Object.values).toBe(native)
        expect(Object.getOwnPropertyDescriptor(Object, 'values')?.writable).toBe(false)

        // Prototype.js pollutes Array.prototype with enumerable methods and reassigns Object.values in
        // sloppy mode; the reassignment must be silently ignored (Reflect.set mirrors that assignment).
        Object.defineProperty(Array.prototype, 'each', { value: () => [], enumerable: true, configurable: true })
        try {
            const forInValues = (obj: object) => {
                const result: unknown[] = []
                for (const key in obj) {
                    result.push((obj as Record<string, unknown>)[key])
                }
                return result
            }
            expect(Reflect.set(Object, 'values', forInValues)).toBe(false)
            expect(Object.values([['x'], ['y']])).toHaveLength(2)
        } finally {
            Reflect.deleteProperty(Array.prototype, 'each')
        }

        // calling again must not throw on the already locked property
        expect(() => restoreNativeGlobals()).not.toThrow()
    })
})

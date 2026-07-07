import { restoreNativeObjectValues } from 'utils/prototypeJsCompat'

// Covers the "clean boot, legacy scripts injected later" order. The opposite order — Prototype.js already
// loaded when the bundle boots — is covered by the polluted-layout test in tableGraphLayout.test.ts.
describe('restoreNativeObjectValues', () => {
    it('keeps the healthy implementation, locks it against later Prototype.js pollution', () => {
        const native = Object.values
        restoreNativeObjectValues()

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
        expect(() => restoreNativeObjectValues()).not.toThrow()
    })
})

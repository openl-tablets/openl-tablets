/**
 * Compatibility shim for pages that load Prototype.js (a RichFaces dependency of the legacy JSF views).
 *
 * Prototype.js replaces two globals the React bundle relies on. Each is restored to its spec behavior and locked.
 * The legacy scripts are injected dynamically, so they may run before or after this bundle: a replacement already
 * in place is overwritten here, and a later one is silently ignored because Prototype.js assigns via sloppy-mode
 * {@code =}, which no-ops on a non-writable property.
 */

/**
 * Pins a global so a later Prototype.js assignment cannot replace it. Locking is one-way: once a property is
 * non-configurable a second call would throw, so an already-pinned global is left alone.
 */
const pin = (target: object, key: string, value: unknown): void => {
    if (Object.getOwnPropertyDescriptor(target, key)?.configurable === false) {
        return
    }
    Object.defineProperty(target, key, { value, writable: false, configurable: false })
}

/**
 * Restores {@code Object.values}.
 *
 * Prototype.js replaces it with a for-in implementation. Because it also adds ~35 enumerable methods to
 * {@code Array.prototype}, that implementation returns array elements followed by those methods. Any library that
 * iterates {@code Object.values(someArray)} then breaks — the dagre engine inside cytoscape-dagre crashes with
 * "t.forEach is not a function" and takes down the table graph (EPBDS-16212).
 */
const restoreObjectValues = (): void => {
    // the native Object.values never returns inherited properties; the Prototype.js for-in version does
    const broken = Object.values(Object.create({ inherited: true })).length > 0
    const specValues = (obj: object): unknown[] => Object.keys(obj).map(key => (obj as Record<string, unknown>)[key])
    pin(Object, 'values', broken ? specValues : Object.values)
}

/**
 * Restores {@code Array.from}.
 *
 * Prototype.js assigns {@code Array.from = $A}, and {@code $A} ignores the map callback: it copies the source and
 * returns it. Every {@code Array.from({ length: n }, fn)} then yields n holes instead of n mapped values, so a
 * generated grid becomes a row of {@code undefined} — which {@code JSON.stringify} writes as {@code null}. The
 * create-table request went out as {@code "source":[[null],[null]]} and the server rejected the missing header.
 */
const restoreArrayFrom = (): void => {
    const broken = Array.from({ length: 1 }, () => 'mapped')[0] !== 'mapped'
    const specFrom = <T, U>(
        source: Iterable<T> | ArrayLike<T>,
        mapFn?: (this: unknown, value: T, index: number) => U,
        thisArg?: unknown
    ): (T | U)[] => {
        const items: (T | U)[] = []
        // The third argument is what the callback sees as `this`, and the pinned property cannot be replaced later.
        const take = (value: T, index: number): void => {
            items.push(mapFn ? mapFn.call(thisArg, value, index) : value)
        }
        if (typeof (source as Iterable<T>)?.[Symbol.iterator] === 'function') {
            let index = 0
            for (const value of source as Iterable<T>) {
                take(value, index++)
            }
        } else {
            // A plain { length } object is not iterable. Every index is visited, including the absent ones: the
            // result must be dense, or the callback would never be called for a hole.
            const length = Math.max(0, Math.trunc(Number((source as ArrayLike<T>)?.length)) || 0)
            // An absent index reads as undefined, exactly as the native Array.from reports it to the callback.
            const indexed = source as Record<number, T>
            for (let index = 0; index < length; index++) {
                take(indexed[index] as T, index)
            }
        }
        return items
    }
    pin(Array, 'from', broken ? specFrom : Array.from)
}

/** Restores every global Prototype.js overwrites. Call once, before anything renders. */
export const restoreNativeGlobals = (): void => {
    restoreObjectValues()
    restoreArrayFrom()
}

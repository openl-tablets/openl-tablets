/**
 * Compatibility shim for pages that load Prototype.js (a RichFaces dependency of the legacy JSF views).
 *
 * Prototype.js replaces {@code Object.values} with a for-in implementation. Because it also adds ~35
 * enumerable methods to {@code Array.prototype}, that implementation returns array elements followed by
 * those methods. Any library that iterates {@code Object.values(someArray)} then breaks — the dagre engine
 * inside cytoscape-dagre crashes with "t.forEach is not a function" and takes down the table graph
 * (EPBDS-16212).
 *
 * Keeps the spec behavior (own enumerable string-keyed values only) and locks the property. The legacy
 * scripts are injected dynamically, so they may run before or after this bundle: a replacement already in
 * place is overwritten here, and a later one is silently ignored because Prototype.js assigns via
 * sloppy-mode {@code =}, which no-ops on a non-writable property.
 */
export const restoreNativeObjectValues = (): void => {
    // the native Object.values never returns inherited properties; the Prototype.js for-in version does
    const broken = Object.values(Object.create({ inherited: true })).length > 0
    const specValues = (obj: object): unknown[] => Object.keys(obj).map(key => (obj as Record<string, unknown>)[key])
    Object.defineProperty(Object, 'values', {
        value: broken ? specValues : Object.values,
        writable: false,
        configurable: false,
    })
}

/**
 * The value a cache holds under a key, created and kept there the first time the key is asked for.
 *
 * The same value is handed out after that, so whatever compares values by identity — a React dependency, an
 * editor reconfigured when its extension changes — sees one value per key.
 */
export const cached = <K, V>(cache: Map<K, V>, key: K, create: () => V): V => {
    const known = cache.get(key)
    if (known !== undefined) {
        return known
    }
    const value = create()
    cache.set(key, value)
    return value
}

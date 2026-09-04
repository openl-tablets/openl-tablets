// Thin, failure-tolerant wrappers around localStorage. A browser that refuses storage (private mode,
// quota, disabled) simply does not remember — every read falls back and every write is a no-op, so
// callers never need their own try/catch.

/** The stored string for a key, or null when absent or storage is unavailable. */
export const readStored = (key: string): string | null => {
    try {
        return localStorage.getItem(key)
    } catch {
        return null
    }
}

/** Stores a string; does nothing when storage is unavailable. */
export const writeStored = (key: string, value: string): void => {
    try {
        localStorage.setItem(key, value)
    } catch {
        // A browser that refuses storage simply does not remember the value.
    }
}

/** Removes a key; does nothing when storage is unavailable. */
export const removeStored = (key: string): void => {
    try {
        localStorage.removeItem(key)
    } catch {
        // Nothing to remember, nothing to remove.
    }
}

/**
 * Reads a JSON value validated by {@link accept}, falling back when the key is missing, unparseable, or
 * fails validation — so a stale or corrupt entry never breaks the caller.
 */
export const readJson = <T>(key: string, fallback: T, accept: (value: unknown) => value is T): T => {
    const stored = readStored(key)
    if (!stored) {
        return fallback
    }
    try {
        const parsed: unknown = JSON.parse(stored)
        return accept(parsed) ? parsed : fallback
    } catch {
        return fallback
    }
}

/** Stores a value as JSON; does nothing when storage is unavailable. */
export const writeJson = (key: string, value: unknown): void => writeStored(key, JSON.stringify(value))

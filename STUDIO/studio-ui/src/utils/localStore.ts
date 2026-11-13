// Thin, failure-tolerant wrappers around the Web Storage. A browser that refuses storage (private mode,
// quota, disabled) simply does not remember — every read falls back and every write is a no-op, so
// callers never need their own try/catch.

/** Where a value is kept: `localStorage` for every tab and visit, `sessionStorage` for the tab alone. */
type StorageArea = 'localStorage' | 'sessionStorage'

/** The stored string for a key, or null when absent or storage is unavailable. */
export const readStored = (key: string, area: StorageArea = 'localStorage'): string | null => {
    try {
        return globalThis[area].getItem(key)
    } catch {
        return null
    }
}

/** Stores a string; does nothing when storage is unavailable. */
export const writeStored = (key: string, value: string, area: StorageArea = 'localStorage'): void => {
    try {
        globalThis[area].setItem(key, value)
    } catch {
        // A browser that refuses storage simply does not remember the value.
    }
}

/** Removes a key; does nothing when storage is unavailable. */
export const removeStored = (key: string, area: StorageArea = 'localStorage'): void => {
    try {
        globalThis[area].removeItem(key)
    } catch {
        // Nothing to remember, nothing to remove.
    }
}

/**
 * Reads a JSON value validated by {@link accept}, falling back when the key is missing, unparseable, or
 * fails validation — so a stale or corrupt entry never breaks the caller.
 */
export const readJson = <T>(key: string, fallback: T, accept: (value: unknown) => value is T): T =>
    storedSetting(key, fallback, jsonText(accept)).read()

/** Stores a value as JSON; does nothing when storage is unavailable. */
export const writeJson = (key: string, value: unknown): void => writeStored(key, JSON.stringify(value))

/** Whether a parsed value is a JSON object, the shape a stored record of fields is read from. */
export const isJsonObject = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value)

/** How a setting is written as text, and read back: undefined for a text that stands for no value of the setting. */
export interface SettingCodec<T> {
    parse: (text: string) => T | undefined
    format: (value: T) => string
}

/** A choice of the reader that the browser keeps between visits. */
export interface StoredSetting<T> {
    /** The key the choice is kept under. */
    readonly key: string
    /** The choice kept, or the default. */
    read: () => T
    /** Keeps the choice, or forgets it when it is the default. */
    write: (value: T) => void
}

/**
 * A choice of the reader that the browser keeps in `localStorage` under a key of its own.
 *
 * Only a choice other than the default is stored: writing the default removes the key, so a browser keeps nothing
 * for a reader who changed nothing, and a later version may change a default its readers never picked.
 *
 * A key that is missing, a browser that refuses storage, and a text that reads as no value — one another version
 * wrote — all give the default.
 */
export const storedSetting = <T>(key: string, defaultValue: T, codec: SettingCodec<T>): StoredSetting<T> => ({
    key,
    read: () => {
        const text = readStored(key)
        return (text === null ? undefined : codec.parse(text)) ?? defaultValue
    },
    write: value => {
        const text = codec.format(value)
        if (text === codec.format(defaultValue)) {
            removeStored(key)
        } else {
            writeStored(key, text)
        }
    },
})

/** A switch, kept as `true` or `false`. */
export const booleanText: SettingCodec<boolean> = {
    parse: text => (text === 'true' || text === 'false' ? text === 'true' : undefined),
    format: String,
}

/** A text kept as it is. */
export const plainText: SettingCodec<string> = { parse: text => text, format: text => text }

/** One of the given texts or numbers, kept as it is written; any other text stands for none. */
export const oneOf = <T extends string | number>(values: readonly T[]): SettingCodec<T> => ({
    parse: text => values.find(value => String(value) === text),
    format: String,
})

/** A whole number within the given bounds; any other text stands for none. */
export const numberBetween = (min: number, max: number): SettingCodec<number> => ({
    parse: text => {
        const value = Number(text)
        return text.trim() !== '' && Number.isInteger(value) && value >= min && value <= max ? value : undefined
    },
    format: String,
})

/**
 * A value kept as JSON, read back only when {@link accept} takes it.
 *
 * Two values count as the same when their JSON is: the default is forgotten only when it is written in the same
 * order of fields as the default itself.
 */
export const jsonText = <T>(accept: (value: unknown) => value is T): SettingCodec<T> => ({
    parse: text => {
        try {
            const value: unknown = JSON.parse(text)
            return accept(value) ? value : undefined
        } catch {
            return undefined
        }
    },
    format: value => JSON.stringify(value),
})

import React, { createContext, useCallback, useContext, useRef, useState } from 'react'

/**
 * What a screen remembers of the parts it draws only while they are in sight, by a key each part names itself with:
 * a page of a list, a value the reader opened, a level read from the server. A part scrolled away and back comes back
 * as the reader left it, and what it read is not read again.
 *
 * A screen that draws everything it holds provides none, and a part keeps its state only while it is drawn.
 */
export const ScreenMemory = createContext<Map<string, unknown> | null>(null)

/**
 * The state of a part of a screen, as `useState` keeps it, and remembered by the screen under the key when the
 * screen provides a {@link ScreenMemory}: a part drawn again starts from what it was.
 *
 * Only a change is remembered, the moment it is made: a part the reader never changed takes no room, and what a read
 * brings after the part has gone out of sight is kept for when it comes back.
 *
 * @param key     the key the part is remembered by, unique on the screen; nothing for a part that is not remembered
 * @param initial the state of a part the screen remembers nothing of
 */
export const useRemembered = <T>(
    key: string | undefined,
    initial: () => T
): [T, (change: React.SetStateAction<T>) => void] => {
    const memory = useContext(ScreenMemory)
    const remembers = memory !== null && key !== undefined
    const [value, setValue] = useState<T>(() => (remembers && memory.has(key) ? memory.get(key) as T : initial()))
    // The state as the last change left it, which a change made after the part is gone starts from as well.
    const latest = useRef(value)
    const change = useCallback((next: React.SetStateAction<T>) => {
        const changed = next instanceof Function ? next(latest.current) : next
        latest.current = changed
        if (remembers) {
            memory.set(key, changed)
        }
        setValue(changed)
    }, [key, memory, remembers])
    return [value, change]
}

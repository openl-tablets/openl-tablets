import { useMemo, useRef } from 'react'

/** Tracks the reloads of one screen. See {@link useLoadGeneration}. */
export interface LoadGeneration {
    /** Begins a reload and returns the generation identifying it. */
    start: (silent: boolean) => number
    /** Whether that reload is still the newest one, and so may put its answer on the screen. */
    isLatest: (generation: number) => boolean
    /** Whether the spinner belongs to that reload, and so is hidden when it ends. */
    ownsSpinner: (generation: number) => boolean
}

/**
 * Tells apart the two things concurrent reloads of a screen compete for: which answer may be shown,
 * and which reload the spinner belongs to.
 *
 * A screen reloads itself from two sides. The user acts and waits behind a spinner, and a background
 * ping re-reads quietly. Both can be in flight at once, and only the newest answer may reach the
 * screen — a stale one would undo the fresh one.
 *
 * The spinner is owned separately: it belongs to the newest reload the user is waiting for. A quiet
 * reload started after it never takes it over, because a quiet reload has no spinner to hide, and
 * whoever waits for one would wait forever.
 */
export function useLoadGeneration(): LoadGeneration {
    const latest = useRef(0)
    const shown = useRef(0)

    return useMemo(() => ({
        start: (silent: boolean) => {
            const generation = ++latest.current
            if (!silent) {
                shown.current = generation
            }
            return generation
        },
        isLatest: (generation: number) => generation === latest.current,
        ownsSpinner: (generation: number) => generation === shown.current,
    }), [])
}

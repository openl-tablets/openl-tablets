import { useRef } from 'react'

/** A reload in flight: which one it is, and when it started. */
export interface Load {
    generation: number
    startedAt: number
}

interface LoadGeneration {
    start: (silent: boolean) => Load
    isLatest: (generation: number) => boolean
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
    // Built once and never rebuilt: both screens keep their `load` callback memoised on it, and a new
    // identity would re-run the mount effect that calls it — a re-read of the whole workspace.
    const api = useRef<LoadGeneration | undefined>(undefined)
    api.current ??= {
        start: (silent: boolean) => {
            const generation = ++latest.current
            if (!silent) {
                shown.current = generation
            }
            return { generation, startedAt: Date.now() }
        },
        isLatest: (generation: number) => generation === latest.current,
        ownsSpinner: (generation: number) => generation === shown.current,
    }
    return api.current
}

import { useCallback, useEffect, useRef, useState } from 'react'

/** The value tracked by {@link useGuardedReload}: `null` while loading, `'error'` on failure, or the loaded data. */
export type GuardedState<T> = T | 'error' | null

/**
 * Loads project-scoped data and keeps it in sync with the current project, guarding against races.
 *
 * A reload is ignored once the component has unmounted or the project has changed, and a slow response
 * from a previous project (or a previous reload of the same project) never overwrites a newer one. The
 * data resets to `null` at the start of each reload so callers can render a loading state.
 *
 * The loader identity is not part of the reload, so an inline `load` callback does not trigger extra
 * reloads — a reload happens on the initial mount, whenever the project changes, and on demand.
 */
export function useGuardedReload<T>(
    projectId: string,
    load: (projectId: string) => Promise<T>
): { data: GuardedState<T>; reload: () => void } {
    const mounted = useRef(false)
    const currentProjectId = useRef(projectId)
    const loadGeneration = useRef(0)
    const loadRef = useRef(load)
    const [data, setData] = useState<GuardedState<T>>(null)

    currentProjectId.current = projectId
    loadRef.current = load

    useEffect(() => {
        mounted.current = true
        return () => {
            mounted.current = false
            loadGeneration.current++
        }
    }, [])

    const reload = useCallback(() => {
        const requestProjectId = projectId
        if (!mounted.current || currentProjectId.current !== requestProjectId) {
            return
        }
        const generation = loadGeneration.current + 1
        loadGeneration.current = generation
        const fresh = () => mounted.current
            && currentProjectId.current === requestProjectId
            && generation === loadGeneration.current
        setData(null)
        loadRef.current(requestProjectId)
            .then(result => {
                if (fresh()) {
                    setData(result)
                }
            })
            .catch(() => {
                if (fresh()) {
                    setData('error')
                }
            })
    }, [projectId])

    useEffect(() => {
        reload()
    }, [reload])

    return { data, reload }
}

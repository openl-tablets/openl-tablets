import { useEffect, useRef, useState } from 'react'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'

/**
 * Live compile status for a project. While {@code enabled}, subscribes to the project-status WebSocket
 * channel, seeding from {@code initial} and applying pushed transitions; while disabled it stays
 * {@code null} without subscribing. {@code initial} is read at subscribe time and does not itself
 * trigger a resubscribe, so callers may recompute it inline on every render.
 */
export function useLiveProjectStatus(
    projectId: string,
    branch: string | null,
    enabled: boolean,
    initial: ProjectStatusUpdate | null
): ProjectStatusUpdate | null {
    const [status, setStatus] = useState<ProjectStatusUpdate | null>(enabled ? initial : null)
    const initialRef = useRef(initial)
    initialRef.current = initial

    useEffect(() => {
        if (!enabled) {
            setStatus(null)
            return
        }
        let cancelled = false
        setStatus(initialRef.current)
        const subscription = subscribeProjectStatus(projectId, branch, update => {
            if (!cancelled) {
                setStatus(update)
            }
        })
        return () => {
            cancelled = true
            subscription.unsubscribe()
        }
    }, [projectId, branch, enabled])

    return status
}

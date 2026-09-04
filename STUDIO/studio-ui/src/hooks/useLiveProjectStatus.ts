import { useEffect, useRef, useState } from 'react'
import { isPushFresherThanRead, subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'

/** A status the channel pushed, with the moment it arrived and the channel it came from. */
interface PushedStatus {
    update: ProjectStatusUpdate
    at: number
    projectId: string
    branch: string | null
}

/** The status shown last, kept beside the value it was computed from so a repeated render is free. */
interface ShownStatus {
    from: ProjectStatusUpdate | null
    status: ProjectStatusUpdate | null
}

const sameStatus = (one: ProjectStatusUpdate | null, other: ProjectStatusUpdate | null): boolean =>
    one === other || JSON.stringify(one) === JSON.stringify(other)

/**
 * Live compile status for a project. While {@code enabled}, subscribes to the project-status WebSocket
 * channel and shows what it pushes; while disabled it stays {@code null} without subscribing.
 *
 * {@code initial} is the status the caller's own read carries, and {@code initialReadAt} is when that
 * read started. A pushed status is shown only while it is the fresher of the two: it wins over a read
 * that started before it arrived, and gives way to a read started after it. Without that, a push would
 * outrank every later answer, and a lost one would leave the project compiling forever. A read that
 * carries no status at all says nothing about it, and leaves a pushed one alone.
 *
 * A status equal to the one already shown keeps its object, so a re-read that changed nothing costs
 * the callers nothing — their message lists and paging are derived from that object.
 */
export function useLiveProjectStatus(
    projectId: string,
    branch: string | null,
    enabled: boolean,
    initial: ProjectStatusUpdate | null,
    initialReadAt = 0
): ProjectStatusUpdate | null {
    const [pushed, setPushed] = useState<PushedStatus | null>(null)
    const shown = useRef<ShownStatus>({ from: null, status: null })

    useEffect(() => {
        setPushed(null)
        if (!enabled) {
            return
        }
        let cancelled = false
        const subscription = subscribeProjectStatus(projectId, branch, update => {
            if (!cancelled) {
                setPushed({ update, at: Date.now(), projectId, branch })
            }
        })
        return () => {
            cancelled = true
            subscription.unsubscribe()
        }
    }, [projectId, branch, enabled])

    if (!enabled) {
        return null
    }
    // A push belongs to the channel it came from. Switching project or branch resubscribes, and the
    // effect that drops the old push runs only after this render, so the channel is checked here too.
    const live = pushed !== null && pushed.projectId === projectId && pushed.branch === branch ? pushed : null
    const fresher = live !== null && (initial === null || isPushFresherThanRead(live.at, initialReadAt))
        ? live.update
        : initial
    // Keyed by the value it came from, so an abandoned render cannot leave an answer behind: a render
    // that computes something else recomputes, and one that computes the same value answers the same.
    if (shown.current.from !== fresher) {
        shown.current = {
            from: fresher,
            status: sameStatus(fresher, shown.current.status) ? shown.current.status : fresher,
        }
    }
    return shown.current.status
}

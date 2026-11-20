import { useEffect, useRef, useState } from 'react'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import {
    fresherStatus,
    isReadBefore,
    isSameStatus,
    subscribeProjectStatus,
    type ProjectStatusUpdate,
} from '../services/projectStatus'
import { getProject } from '../services/repositories'

/** Something the hook learned on the channel of a project and a branch. */
interface OnChannel {
    projectId: string
    branch: string | null
}

/** The freshest status the hook got by itself: pushed, or read again. */
interface OwnStatus extends OnChannel {
    update: ProjectStatusUpdate
}

/** The number the subscription last started at. */
interface Started extends OnChannel {
    sequence: number
}

/** The status shown last, kept beside the value it was computed from so a repeated render is free. */
interface ShownStatus {
    from: ProjectStatusUpdate | null
    status: ProjectStatusUpdate | null
}

/**
 * Live compile status for a project. While {@code enabled}, subscribes to the project-status WebSocket
 * channel and shows what it pushes; while disabled it stays {@code null} without subscribing.
 *
 * {@code initial} is the status the caller's own read carries. The hook shows the fresher of it and of what it
 * got by itself: the server numbers every status it reads, and the larger number wins, whichever arrived last. A
 * read that carries no status at all says nothing about it, and leaves a pushed one alone.
 *
 * A change told before the subscription started — on the first subscription, or while the connection was down —
 * reaches nobody. So once the subscription starts, a status read before that is read once more: a compilation
 * that began or ended unheard is not shown wrong for good. While no status is known yet, the caller's read is on
 * its way, and the check waits for it.
 *
 * A status equal to the one already shown keeps its object, so a re-read that changed nothing costs
 * the callers nothing — their message lists and paging are derived from that object.
 */
export function useLiveProjectStatus(
    projectId: string,
    branch: string | null,
    enabled: boolean,
    initial: ProjectStatusUpdate | null
): ProjectStatusUpdate | null {
    const [own, setOwn] = useState<OwnStatus | null>(null)
    const [started, setStarted] = useState<Started | null>(null)
    const shown = useRef<ShownStatus>({ from: null, status: null })
    // Counts the subscriptions, so that a read answering for an earlier one is dropped.
    const subscriptions = useRef(0)
    // The start of the subscription a status was read again for, so that it is read once per start.
    const readAgainFor = useRef<number | null>(null)

    useEffect(() => {
        setOwn(null)
        setStarted(null)
        readAgainFor.current = null
        subscriptions.current += 1
        if (!enabled) {
            return
        }
        let cancelled = false
        const subscription = subscribeProjectStatus(
            projectId,
            branch,
            update => {
                if (!cancelled) {
                    setOwn(previous => keepFresher(previous, { update, projectId, branch }))
                }
            },
            sequence => {
                if (!cancelled) {
                    setStarted({ sequence, projectId, branch })
                }
            }
        )
        return () => {
            cancelled = true
            subscription.unsubscribe()
        }
    }, [projectId, branch, enabled])

    // A push belongs to the channel it came from. Switching project or branch resubscribes, and the
    // effect that drops the old push runs only after this render, so the channel is checked here too.
    const live = onChannel(own, projectId, branch)?.update ?? null
    const startedAt = onChannel(started, projectId, branch)?.sequence ?? null
    const fresher = enabled ? fresherStatus(initial, live) : null
    const readBeforeStart = fresher !== null && startedAt !== null && isReadBefore(fresher, startedAt)

    useEffect(() => {
        if (!readBeforeStart || startedAt === null || readAgainFor.current === startedAt) {
            return
        }
        readAgainFor.current = startedAt
        const subscription = subscriptions.current
        getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
            .then(read => {
                const update = read.compileStatus
                if (update && subscription === subscriptions.current) {
                    setOwn(previous => keepFresher(previous, { update, projectId, branch }))
                }
            })
            .catch(() => {
                // The next push, or the next start of the subscription, tells the status instead.
            })
    }, [readBeforeStart, startedAt, projectId, branch])

    if (fresher === null) {
        return null
    }
    // Keyed by the value it came from, so an abandoned render cannot leave an answer behind: a render
    // that computes something else recomputes, and one that computes the same value answers the same.
    if (shown.current.from !== fresher) {
        shown.current = {
            from: fresher,
            status: isSameStatus(fresher, shown.current.status) ? shown.current.status : fresher,
        }
    }
    return shown.current.status
}

function onChannel<T extends OnChannel>(value: T | null, projectId: string, branch: string | null): T | null {
    return value !== null && value.projectId === projectId && value.branch === branch ? value : null
}

/** The fresher of the two; a status of another channel gives way to any status of this one. */
function keepFresher(previous: OwnStatus | null, next: OwnStatus): OwnStatus {
    const same = onChannel(previous, next.projectId, next.branch)
    return same !== null && fresherStatus(same.update, next.update) === same.update ? same : next
}

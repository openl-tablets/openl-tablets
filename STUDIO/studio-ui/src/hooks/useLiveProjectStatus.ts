import { useCallback, useEffect, useRef, useState } from 'react'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { isPushFresherThanRead, subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'
import { getProject } from '../services/repositories'
import webSocketService from '../services/websocket'

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

/**
 * How long a caller that waits for the status hears nothing — no push, no read — before the status is read again.
 */
export const STATUS_QUIET_SPELL_MS = 5000

/** The read of the status alone: the project answers with its compile status and no other field. */
const STATUS_ONLY = { includes: ['status'], fields: 'compileStatus' } satisfies Parameters<typeof getProject>[1]

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
 * Nothing pushed while the connection is down arrives later, so the status is read afresh on every reconnect, as a
 * push: a compilation that ended meanwhile, or one the restart of the server ended, is not shown as running for good.
 *
 * Nor does anything arrive that is pushed before the server has made the subscription: a compilation can end between
 * the caller's read and the subscription, and the socket says nothing of it. So while the caller is {@code waiting}
 * — for its module to be compiled, say — the status is read again after every quiet spell of
 * {@link STATUS_QUIET_SPELL_MS} in which nothing was pushed or read, the socket connected or not.
 *
 * Either read asks the server for the compile status alone, not for the rest of the project.
 *
 * A status equal to the one already shown keeps its object, so a re-read that changed nothing costs
 * the callers nothing — their message lists and paging are derived from that object.
 */
export function useLiveProjectStatus(
    projectId: string,
    branch: string | null,
    enabled: boolean,
    initial: ProjectStatusUpdate | null,
    initialReadAt = 0,
    waiting = false
): ProjectStatusUpdate | null {
    const [pushed, setPushed] = useState<PushedStatus | null>(null)
    // Counts the reads that ended, so a read that failed or told no status starts the next quiet spell too
    const [readsEnded, setReadsEnded] = useState(0)
    const shown = useRef<ShownStatus>({ from: null, status: null })

    // Reads the status as a push, and nothing else of the project. An answer about a project or a branch the screen
    // moved away from is not shown: a push belongs to the channel it came from.
    const readAfresh = useCallback(() => {
        const readAt = Date.now()
        getProject(projectId, STATUS_ONLY, LOCAL_LOAD_API_OPTIONS)
            .then(read => {
                const update = read.compileStatus
                if (update) {
                    // A push that arrived while the read was on its way is the fresher of the two.
                    setPushed(shownNow => (shownNow !== null && shownNow.at > readAt
                        ? shownNow
                        : { update, at: readAt, projectId, branch }))
                }
            })
            .catch(() => {
                // The next push, the next reconnect or the next quiet spell tells the status instead.
            })
            .finally(() => setReadsEnded(count => count + 1))
    }, [projectId, branch])

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
        const stopListening = webSocketService.onReconnect(readAfresh)
        return () => {
            cancelled = true
            subscription.unsubscribe()
            stopListening()
        }
    }, [projectId, branch, enabled, readAfresh])

    // Every push and every read starts the spell over, so a compilation heard going on is not read again.
    useEffect(() => {
        if (!enabled || !waiting) {
            return undefined
        }
        const spell = setTimeout(readAfresh, STATUS_QUIET_SPELL_MS)
        return () => clearTimeout(spell)
    }, [enabled, waiting, readAfresh, pushed, initial, readsEnded])

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
